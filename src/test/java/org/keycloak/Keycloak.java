package org.keycloak;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThreadFactory;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyBuilder;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.keycloak.common.Version;
import org.keycloak.common.crypto.FipsMode;
import org.keycloak.config.HttpOptions;
import org.keycloak.config.LoggingOptions;
import org.keycloak.config.Option;
import org.keycloak.config.SecurityOptions;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.KeycloakMain;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.IgnoredArtifacts;

/**
 * Patched version of org.keycloak.Keycloak from keycloak-junit5 26.6.2.
 *
 * Adds Configuration.resetConfig() inside initSys() to work around a Keycloak 26.6.2 bug
 * where bootstrap() triggers Configuration.getConfig() before initSys() calls initConfig(),
 * causing an IllegalStateException("Config should not be initialized until profile is determined").
 */
public class Keycloak {

    private CuratedApplication curated;
    private RunningQuarkusApplication application;
    private ApplicationModel applicationModel;
    private Path homeDir;
    private List<Dependency> dependencies;
    private boolean fipsEnabled;
    private Properties systemProperties;

    public static void main(String[] args) {
        Keycloak.builder().start(args);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Keycloak() {
        this(null, Version.VERSION, List.of(), false);
    }

    public Keycloak(Path homeDir, String version, List<Dependency> dependencies, boolean fipsEnabled) {
        this.homeDir = homeDir;
        this.dependencies = dependencies;
        this.fipsEnabled = fipsEnabled;
        try {
            this.applicationModel = this.createApplicationModel(version);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Keycloak start(List<String> args) {
        this.systemProperties = (Properties) System.getProperties().clone();
        QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder()
                .setExistingModel(this.applicationModel)
                .setApplicationRoot(this.applicationModel.getApplicationModule().getModuleDir().toPath())
                .setTargetDirectory(this.applicationModel.getApplicationModule().getModuleDir().toPath())
                .setIsolateDeployment(true)
                .setFlatClassPath(true)
                .setMode(QuarkusBootstrap.Mode.TEST);
        try {
            this.curated = builder.build().bootstrap();
            AugmentAction action = this.curated.createAugmentor();
            Environment.setHomeDir(this.homeDir);
            if (!Keycloak.initSys(args.toArray(String[]::new))) {
                return this;
            }
            System.setProperty("kc.test.rebuild", "true");
            StartupAction startupAction = action.createInitialRuntimeApplication();
            System.getProperties().remove("kc.test.rebuild");
            this.application = startupAction.runMainClass(args.toArray(new String[0]));
            return this;
        } catch (Exception cause) {
            throw new RuntimeException("Failed to start the server", cause);
        }
    }

    public void stop() throws TimeoutException {
        try {
            if (this.isRunning()) {
                this.closeApplication();
            }
        } finally {
            if (this.systemProperties != null) {
                KeycloakMain.reset(this.systemProperties);
                this.systemProperties = null;
            } else {
                // Fallback reset when start() failed before systemProperties was cloned
                Configuration.resetConfig();
            }
        }
    }

    private ApplicationModel createApplicationModel(String keycloakVersion) throws AppModelResolverException {
        BootstrapAppModelResolver appModelResolver = new BootstrapAppModelResolver(this.getMavenArtifactResolver());
        WorkspaceModule module = this.createWorkspaceModule(keycloakVersion);
        return appModelResolver.resolveModel(module);
    }

    private WorkspaceModule createWorkspaceModule(String keycloakVersion) {
        Path moduleDir = Keycloak.createModuleDir();
        DependencyBuilder serverDependency = DependencyBuilder.newInstance()
                .setGroupId("org.keycloak")
                .setArtifactId("keycloak-quarkus-server")
                .setVersion(keycloakVersion)
                .addExclusion("org.jboss.logmanager", "log4j-jboss-logmanager");
        if (this.fipsEnabled) {
            IgnoredArtifacts.FIPS_ENABLED.stream().map(s -> s.split(":"))
                    .forEach(d -> serverDependency.addExclusion(d[0], d[1]));
        } else {
            IgnoredArtifacts.FIPS_DISABLED.stream().map(s -> s.split(":"))
                    .forEach(d -> serverDependency.addExclusion(d[0], d[1]));
        }
        WorkspaceModule.Mutable moduleBuilder = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.keycloak", "keycloak-embedded", "1"))
                .setModuleDir(moduleDir)
                .setBuildDir(moduleDir)
                .addDependencyConstraint(Dependency.pomImport("org.keycloak", "keycloak-quarkus-parent", keycloakVersion))
                .addDependency(serverDependency.build());
        for (Dependency dependency : this.dependencies) {
            moduleBuilder.addDependency(dependency);
        }
        return moduleBuilder.build();
    }

    private static Path createModuleDir() {
        try {
            return Files.createTempDirectory("kc-embedded");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    MavenArtifactResolver getMavenArtifactResolver() throws BootstrapMavenException {
        return MavenArtifactResolver.builder()
                .setWorkspaceDiscovery(true)
                .setOffline(false)
                .build();
    }

    private boolean isRunning() {
        return this.application != null;
    }

    private void closeApplication() {
        if (this.application != null) {
            try {
                this.application.close();
            } catch (Exception cause) {
                cause.printStackTrace();
            }
        }
        QuarkusConfigFactory.setConfig(null);
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            ConfigProviderResolver cpr = ConfigProviderResolver.instance();
            cpr.releaseConfig(cpr.getConfig());
        } catch (Throwable throwable) {
            // ignore
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
        this.application = null;
        this.curated = null;
    }

    /**
     * Patched version: adds Configuration.resetConfig() before picocli.parseAndRun()
     * to fix Keycloak 26.6.2 bug where bootstrap() initializes Configuration.config
     * before initSys() can check the profile.
     */
    public static boolean initSys(String... args) {
        // PATCH: Reset config before parsing to allow initConfig() to pass its isInitialized() check.
        // bootstrap()/createAugmentor() may initialize Configuration.config, but initConfig() requires it null.
        Configuration.resetConfig();
        final AtomicBoolean result = new AtomicBoolean();
        Picocli picocli = new Picocli() {
            @Override
            public void build() throws Throwable {
            }

            @Override
            public void start() {
                throw new AssertionError();
            }

            @Override
            public void exit(int exitCode) {
                result.set(exitCode == 10);
            }
        };
        picocli.parseAndRun(List.of(args));
        System.setProperty("kc.config.built", "true");
        return result.get();
    }

    public static Path initTempDirectory(String name) {
        String buildDir = System.getProperty("project.build.directory");
        if (buildDir == null) {
            try {
                return Files.createTempDirectory(name).toAbsolutePath();
            } catch (IOException e) {
                throw new RuntimeException("Could not create temporary directory", e);
            }
        }
        Path homeDir = Path.of(buildDir, name);
        // Replacement for FileUtils.deleteDirectory() without commons-io dependency
        if (homeDir.toFile().exists()) {
            try {
                Files.walk(homeDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return homeDir;
    }

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        System.setProperty("quarkus.http.test-port", "${kc.http-port}");
        System.setProperty("quarkus.http.test-ssl-port", "${kc.https-port}");
        System.setProperty("java.util.concurrent.ForkJoinPool.common.threadFactory",
                QuarkusForkJoinWorkerThreadFactory.class.getName());
    }

    public static class Builder {

        private String version;
        private Path homeDir;
        private List<Dependency> dependencies = new ArrayList<>();

        private Builder() {
        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder setHomeDir(Path path) {
            this.homeDir = path;
            return this;
        }

        public Builder addDependency(String groupId, String artifactId, String version) {
            this.addDependency(groupId, artifactId, version, null);
            return this;
        }

        public Builder addDependency(String groupId, String artifactId, String version, String classifier) {
            this.dependencies.add(DependencyBuilder.newInstance()
                    .setGroupId(groupId)
                    .setArtifactId(artifactId)
                    .setVersion(version)
                    .setClassifier(classifier)
                    .build());
            return this;
        }

        public Keycloak start(String... args) {
            return this.start(List.of(args));
        }

        public Keycloak start(List<String> rawArgs) {
            if (this.homeDir == null) {
                this.homeDir = Keycloak.initTempDirectory("keycloak-home");
            }
            ArrayList<String> args = new ArrayList<>(rawArgs);
            if (args.isEmpty()) {
                args.add("start-dev");
            }
            this.addOptionIfNotSet(args, HttpOptions.HTTP_ENABLED, true);
            this.addOptionIfNotSet(args, HttpOptions.HTTP_PORT);
            this.addOptionIfNotSet(args, HttpOptions.HTTPS_PORT);
            boolean isFipsEnabled = Optional.ofNullable(this.getOptionValue(args, SecurityOptions.FIPS_MODE))
                    .map(FipsMode::valueOfOption)
                    .orElse(FipsMode.DISABLED)
                    .isFipsEnabled();
            if (isFipsEnabled && this.getOptionValue(args, LoggingOptions.LOG_LEVEL) == null) {
                args.add("--log-level=org.keycloak.common.crypto:TRACE,org.keycloak.crypto:TRACE");
            }
            return new Keycloak(this.homeDir, this.version, this.dependencies, isFipsEnabled).start(args);
        }

        private <T> void addOptionIfNotSet(List<String> args, Option<T> option) {
            this.addOptionIfNotSet(args, option, null);
        }

        private <T> void addOptionIfNotSet(List<String> args, Option<T> option, T defaultValue) {
            String value = this.getOptionValue(args, option);
            if (value == null) {
                T resolvedDefault = Optional.ofNullable(defaultValue).orElseGet(option.getDefaultValue()::get);
                args.add(Configuration.toCliFormat(option.getKey()) + "=" + Option.getDefaultValueString(resolvedDefault));
            }
        }

        private String getOptionValue(List<String> args, Option<?> option) {
            for (String arg : args) {
                if (!arg.contains(option.getKey())) continue;
                if (arg.endsWith(option.getKey())) {
                    throw new IllegalArgumentException(
                            "Option '" + arg + "' value must be set using '=' as a separator");
                }
                return arg.substring("--".length() + option.getKey().length() + 1);
            }
            return null;
        }
    }
}
