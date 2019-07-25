package valandur.webapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import io.swagger.converter.ModelConverters;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.bstats.sponge.Metrics2;
import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Platform.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.*;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.service.ServiceManager;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import valandur.webapi.block.BlockOperation;
import valandur.webapi.block.BlockOperationStatusChangeEvent;
import valandur.webapi.block.BlockService;
import valandur.webapi.cache.CacheService;
import valandur.webapi.command.CommandRegistry;
import valandur.webapi.command.CommandSource;
import valandur.webapi.config.BaseConfig;
import valandur.webapi.config.IMainConfig;
import valandur.webapi.config.MainConfig;
import valandur.webapi.hook.WebHook;
import valandur.webapi.hook.WebHookSerializer;
import valandur.webapi.hook.WebHookService;
import valandur.webapi.link.LinkService;
import valandur.webapi.link.internal.InternalHttpRequest;
import valandur.webapi.link.internal.InternalHttpResponse;
import valandur.webapi.link.message.RequestMessage;
import valandur.webapi.link.message.ResponseMessage;
import valandur.webapi.message.InteractiveMessageService;
import valandur.webapi.security.PermissionStruct;
import valandur.webapi.security.PermissionStructSerializer;
import valandur.webapi.security.SecurityService;
import valandur.webapi.serialize.SerializeService;
import valandur.webapi.server.ServerService;
import valandur.webapi.servlet.base.BaseServlet;
import valandur.webapi.servlet.base.ServletService;
import valandur.webapi.swagger.SwaggerModelConverter;
import valandur.webapi.user.UserPermissionStruct;
import valandur.webapi.user.UserPermissionStructSerializer;
import valandur.webapi.user.UserService;
import valandur.webapi.util.JettyLogger;
import valandur.webapi.util.Timings;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Plugin(
        id = Constants.ID,
        version = Constants.VERSION,
        name = Constants.NAME,
        url = Constants.URL,
        description = Constants.DESCRIPTION,
        authors = {
                "Valandur"
        }
)
public class WebAPI implements IPlugin {
    private static WebAPI instance;
    public static WebAPI getInstance() {
        return WebAPI.instance;
    }

    private static SpongeExecutorService syncExecutor;
    private static SpongeExecutorService asyncExecutor;
    public static SpongeExecutorService getAsyncExecutor() {
        return WebAPI.asyncExecutor;
    }

    private MainConfig mainConfig;
    @Override
    public IMainConfig getMainConfig() {
        return mainConfig;
    }

    private boolean devMode = false;
    public static boolean isDevMode() {
        return WebAPI.getInstance().devMode;
    }

    private boolean adminPanelEnabled = true;
    public static boolean isAdminPanelEnabled() {
        return WebAPI.getInstance().adminPanelEnabled;
    }

    private static String spongeApi;
    private static String spongeGame;
    private static String spongeImpl;
    private static String pluginList;

    private static WebServer server;

    @Inject
    private Metrics2 metrics;

    @Inject
    private Logger logger;
    public Logger getLogger() {
        return this.logger;
    }

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configPath;
    public static Path getConfigPath() {
        return WebAPI.getInstance().configPath;
    }

    @Inject
    private PluginContainer container;
    public static PluginContainer getContainer() {
        return WebAPI.getInstance().container;
    }

    private boolean reportErrors;
    public static boolean reportErrors() {
        return WebAPI.getInstance() == null || WebAPI.getInstance().reportErrors;
    }

    // Services
    private BlockService blockService;
    public static BlockService getBlockService() {
        return WebAPI.getInstance().blockService;
    }

    private CacheService cacheService;
    public static CacheService getCacheService() {
        return WebAPI.getInstance().cacheService;
    }

    private SerializeService serializeService;
    public static SerializeService getSerializeService() {
        return WebAPI.getInstance().serializeService;
    }

    private InteractiveMessageService messageService;
    public static InteractiveMessageService getMessageService() {
        return WebAPI.getInstance().messageService;
    }

    private LinkService linkService;
    public static LinkService getLinkService() {
        return WebAPI.getInstance().linkService;
    }

    private SecurityService securityService;
    public static SecurityService getSecurityService() {
        return WebAPI.getInstance().securityService;
    }

    private ServerService serverService;
    public static ServerService getServerService() {
        return WebAPI.getInstance().serverService;
    }

    private ServletService servletService;
    public static ServletService getServletService() {
        return WebAPI.getInstance().servletService;
    }

    private UserService userService;
    public static UserService getUserService() {
        return WebAPI.getInstance().userService;
    }

    private WebHookService webHookService;
    public static WebHookService getWebHookService() {
        return WebAPI.getInstance().webHookService;
    }



    public WebAPI() {
        System.setProperty("sentry.dsn", "https://fb64795d2a5c4ff18f3c3e4117d7c245:53cf4ea85ae44608ab5b189f0c07b3f1@sentry.io/203545");
        System.setProperty("sentry.release", Constants.VERSION.split("-")[0]);
        System.setProperty("sentry.maxmessagelength", "2000");
        System.setProperty("sentry.stacktrace.app.packages", WebAPI.class.getPackage().getName());
    }

    @Listener
    public void onPreInitialization(GamePreInitializationEvent event) {
        WebAPI.instance = this;

        Timings.STARTUP.startTiming();

        Platform platform = Sponge.getPlatform();
        spongeApi = platform.getContainer(Component.API).getVersion().orElse(null);
        spongeGame = platform.getContainer(Component.GAME).getVersion().orElse(null);
        spongeImpl = platform.getContainer(Component.IMPLEMENTATION).getVersion().orElse(null);
        pluginList = Sponge.getPluginManager().getPlugins().stream()
                .map(p -> p.getId() + ": " + p.getVersion().orElse(null))
                .collect(Collectors.joining("; \n"));

        // Create our config directory if it doesn't exist
        if (!Files.exists(configPath)) {
            try {
                Files.createDirectories(configPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Reusable sync executor to run code on main server thread
        syncExecutor = Sponge.getScheduler().createSyncExecutor(this);
        asyncExecutor = Sponge.getScheduler().createAsyncExecutor(this);

        // Register custom serializers
        TypeSerializers.getDefaultSerializers().registerType(
                TypeToken.of(WebHook.class), new WebHookSerializer());
        TypeSerializers.getDefaultSerializers().registerType(
                TypeToken.of(UserPermissionStruct.class), new UserPermissionStructSerializer());
        TypeSerializers.getDefaultSerializers().registerType(
                TypeToken.of(PermissionStruct.class), new PermissionStructSerializer());

        // Setup services
        this.blockService = new BlockService();
        this.cacheService = new CacheService();
        this.linkService = new LinkService();
        this.messageService = new InteractiveMessageService();
        this.securityService = new SecurityService();
        this.serializeService = new SerializeService();
        this.serverService = new ServerService();
        this.servletService = new ServletService();
        this.webHookService = new WebHookService();
        this.userService = new UserService();

        // Register services
        ServiceManager serviceMan = Sponge.getServiceManager();
        serviceMan.setProvider(this, BlockService.class, blockService);
        serviceMan.setProvider(this, CacheService.class, cacheService);
        serviceMan.setProvider(this, LinkService.class, linkService);
        serviceMan.setProvider(this, InteractiveMessageService.class, messageService);
        serviceMan.setProvider(this, SecurityService.class, securityService);
        serviceMan.setProvider(this, SerializeService.class, serializeService);
        serviceMan.setProvider(this, ServerService.class, serverService);
        serviceMan.setProvider(this, ServletService.class, servletService);
        serviceMan.setProvider(this, WebHookService.class, webHookService);
        serviceMan.setProvider(this, UserService.class, userService);

        // Register events of services
        EventManager evenMan = Sponge.getEventManager();
        evenMan.registerListeners(this, cacheService);
        evenMan.registerListeners(this, linkService);
        evenMan.registerListeners(this, webHookService);

        // Swagger setup stuff
        ModelConverters.getInstance().addConverter(new SwaggerModelConverter());

        Timings.STARTUP.stopTiming();
    }
    @Listener
    public void onInitialization(GameInitializationEvent event) {
        Timings.STARTUP.startTiming();

        this.printInfo();

        logger.info("Setting up jetty logger...");
        Log.setLog(new JettyLogger());

        // Main init function, that is also called when reloading the plugin
        init(null);

        logger.info(Constants.NAME + " ready");

        Timings.STARTUP.stopTiming();
    }
    @Listener(order = Order.POST)
    public void onPostInitialization(GamePostInitializationEvent event) {
        Timings.STARTUP.startTiming();

        // Load base data
        cacheService.updateWorlds();
        cacheService.updatePlugins();
        cacheService.updateCommands();

        Timings.STARTUP.stopTiming();
    }

    private void printInfo() {
        int width = 33;
        String line = String.join("", Collections.nCopies(width, "-"));

        String name = Constants.NAME;
        int nameSpace = width - name.length() - 2;
        String namePre = String.join("", Collections.nCopies((int) Math.floor(nameSpace / 2d), " "));
        String namePost = String.join("", Collections.nCopies((int) Math.ceil(nameSpace / 2d), " "));

        String version = Constants.VERSION;
        int versionSpace = width - version.length() - 2;
        String versionPre = String.join("", Collections.nCopies((int) Math.floor(versionSpace / 2d), " "));
        String versionPost = String.join("", Collections.nCopies((int) Math.ceil(versionSpace / 2d), " "));

        logger.info(line);
        logger.info("|" + namePre + Constants.NAME + namePost + "|");
        logger.info("|" + versionPre + Constants.VERSION + versionPost + "|");
        logger.info(line);
    }

    // Reusable setup function, for starting and reloading
    private void init(Player triggeringPlayer) {
        Timings.STARTUP.startTiming();

        logger.info("Loading configuration...");

        Path configPath = WebAPI.getConfigPath().resolve("config.conf").normalize();
        this.mainConfig = BaseConfig.load(configPath, new MainConfig());

        // Save important config values to variables
        devMode = mainConfig.devMode;
        reportErrors = mainConfig.reportErrors;
        adminPanelEnabled = mainConfig.adminPanel;

        // Create our WebServer
        server = new WebServer(this);

        if (devMode) {
            logger.warn("Web-API IS RUNNING IN DEV MODE. ERROR REPORTING IS OFF!");
            reportErrors = false;
        }

        // Use the container instead of VERSION so the compiler doesn't remove it because it's constant
        if (this.container.getVersion().orElse("").equalsIgnoreCase("@version@")) {
            logger.warn("Web-API VERSION SIGNALS DEV MODE. ERROR REPORTING IS OFF!");
            reportErrors = false;
        }

        /*if (reportErrors) {
            Sentry.init();
        }*/

        blockService.init();

        cacheService.init();

        linkService.init();

        securityService.init();

        serializeService.init();

        serverService.init();

        servletService.init();

        userService.init();

        webHookService.init();

        CommandRegistry.init();

        if (triggeringPlayer != null) {
            triggeringPlayer.sendMessage(Text.builder().color(TextColors.AQUA)
                    .append(Text.of("[" + Constants.NAME + "] " + Constants.NAME + " has been reloaded!"))
                    .build());
        }

        Timings.STARTUP.stopTiming();
    }

    private void checkForUpdates() {
        if (devMode) {
            logger.warn("SKIPPING UPDATE CHECK IN DEV MODE");
            return;
        }

        asyncExecutor.execute(() -> {
            HttpsURLConnection connection = null;
            try {
                java.net.URL url = new URL(Constants.UPDATE_URL);
                connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Web-API");
                connection.setRequestProperty("accept", "application/json");
                connection.setRequestProperty("charset", "utf-8");
                connection.setUseCaches(false);

                //Get Response
                int code = connection.getResponseCode();
                if (code != 200) {
                    logger.warn("Could not check for updates: " + code);
                    return;
                }

                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\r');
                }
                rd.close();

                String respString = response.toString().trim();
                if (respString.isEmpty() || respString.equalsIgnoreCase("OK")) {
                    logger.warn("Empty response received when checking for updates");
                    return;
                }

                ObjectMapper map = new ObjectMapper();
                JsonNode resp = map.readTree(respString);

                String version = container.getVersion().orElse("").split("-")[0];
                String newVersion = resp.get("tag_name").asText().substring(1).split("-")[0];

                if (newVersion.equalsIgnoreCase(version)) {
                    return;
                }

                String[] changes = resp.get("body").asText().split("\n");

                logger.warn("------- Web-API Update -------");
                logger.warn("Latest: " + newVersion);
                logger.warn("Current: " + version);
                logger.warn("---------- Changes -----------");
                for (String change : changes)
                    logger.warn(change);
                logger.warn("------------------------------");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    // Event listeners
    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        server.start();

        checkForUpdates();

        // Add custom bstats metrics
        metrics.addCustomChart(new Metrics2.DrilldownPie("plugin_version", () -> {
            String[] vers = Constants.VERSION.split("-");

            Map<String, Integer> entry = new HashMap<>();
            entry.put(vers[1], 1);

            Map<String, Map<String, Integer>> map = new HashMap<>();
            map.put(vers[0], entry);

            return map;
        }));
        metrics.addCustomChart(new Metrics2.SimplePie("report_errors", () -> reportErrors ? "Yes" : "No"));
        metrics.addCustomChart(new Metrics2.SimplePie("admin_panel", () -> adminPanelEnabled ? "Yes" : "No"));
        metrics.addCustomChart(new Metrics2.SimpleBarChart("servlets", () -> {
            Map<String, Integer> map = new HashMap<>();
            Collection<Class<? extends BaseServlet>> servlets = servletService.getRegisteredServlets().values();
            for (Class<? extends BaseServlet> servlet : servlets) {
                map.put(servlet.getName(), 1);
            }
            return map;
        }));
    }
    @Listener
    public void onServerStop(GameStoppedServerEvent event) {
        server.stop();
    }
    @Listener
    public void onReload(GameReloadEvent event) {
        Optional<Player> p = event.getCause().first(Player.class);

        logger.info("Reloading " + Constants.NAME + " v" + Constants.VERSION + "...");

        cacheService.updatePlugins();
        cacheService.updateCommands();

        server.stop();

        init(p.orElse(null));

        p.ifPresent(player -> player.sendMessage(Text.builder()
                .color(TextColors.AQUA)
                .append(Text.of("[" + Constants.NAME + "] The web server has been restarted!"))
                .toText()
        ));

        checkForUpdates();

        logger.info("Reloaded " + Constants.NAME);
    }

    @Listener(order = Order.POST)
    public void onBlockUpdateStatusChange(BlockOperationStatusChangeEvent event) {
        BlockOperation on = event.getBlockOperation();
        switch (on.getStatus()) {
            case DONE:
                logger.info("Block op " + on.getUUID() + " is done");
                break;

            case ERRORED:
                logger.warn("Block op " + on.getUUID() + " failed: " + on.getError());
                break;

            case CANCELED:
                logger.info("Block op " + on.getUUID() + " was canceled");
                break;

            case PAUSED:
                logger.info("Block op " + on.getUUID() + " paused");
                break;

            case RUNNING:
                logger.info("Block op " + on.getUUID() + " started");
                break;
        }
    }

    // Execute a command
    public static CommandResult executeCommand(String command, CommandSource source) {
        CommandManager cmdManager = Sponge.getGame().getCommandManager();
        return cmdManager.process(source, command);
    }

    // Run functions on the main server thread
    public static void runOnMain(Runnable runnable) throws WebApplicationException {
        if (Sponge.getServer().isMainThread()) {
            runnable.run();
        } else {
            CompletableFuture future = CompletableFuture.runAsync(runnable, WebAPI.syncExecutor);
            try {
                future.get();
            } catch (InterruptedException ignored) {
            } catch (ExecutionException e) {
                // Rethrow any web application exceptions we get, because they're handled by the servlets
                if (e.getCause() instanceof WebApplicationException)
                    throw (WebApplicationException)e.getCause();

                e.printStackTrace();
                WebAPI.sentryCapture(e);
                throw new InternalServerErrorException(e.getMessage());
            }
        }
    }
    public static <T> T runOnMain(Supplier<T> supplier) throws WebApplicationException {
        if (Sponge.getServer().isMainThread()) {
            Timings.RUN_ON_MAIN.startTiming();
            T obj = supplier.get();
            Timings.RUN_ON_MAIN.stopTiming();
            return obj;
        } else {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(supplier, WebAPI.syncExecutor);
            try {
                return future.get();
            } catch (InterruptedException e) {
                throw new InternalServerErrorException(e.getMessage());
            } catch (ExecutionException e) {
                // Rethrow any web application exceptions we get, because they're handled by the servlets
                if (e.getCause() instanceof WebApplicationException)
                    throw (WebApplicationException)e.getCause();

                e.printStackTrace();
                WebAPI.sentryCapture(e);
                throw new InternalServerErrorException(e.getMessage());
            }
        }
    }

    // Emulate HTTP requests from sockets
    public static ResponseMessage emulateRequest(RequestMessage message) {
        try {
            InternalHttpRequest req = new InternalHttpRequest(message);
            InternalHttpResponse res = new InternalHttpResponse();
            server.handle(message.getPath(), req, req, res);
            return new ResponseMessage(message.getId(), res.getStatus(), res.getHeaders(), res.getOuput());
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseMessage(message.getId(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    new HashMap<>(), e.getMessage());
        }
    }

    // Sentry logging
    private static void addDefaultContext() {
        /*ontext context = Sentry.getContext();
        context.addTag("full_release", Constants.VERSION);

        context.addTag("java_version", System.getProperty("java.version"));
        context.addTag("os_name", System.getProperty("os.name"));
        context.addTag("os_arch", System.getProperty("os.arch"));
        context.addTag("os_version", System.getProperty("os.version"));

        context.addExtra("processors", Runtime.getRuntime().availableProcessors());
        context.addExtra("memory_max", Runtime.getRuntime().maxMemory());
        context.addExtra("memory_total", Runtime.getRuntime().totalMemory());
        context.addExtra("memory_free", Runtime.getRuntime().freeMemory());

        context.addExtra("server_host", server.getHost());
        context.addExtra("server_port_http", server.getHttpPort());
        context.addExtra("server_port_https", server.getHttpsPort());

        context.addExtra("sponge_api", spongeApi);
        context.addExtra("sponge_game", spongeGame);
        context.addExtra("sponge_impl", spongeImpl);

        context.addExtra("plugins", pluginList);*/
    }
    public static void sentryCapture(Exception e) {
        if (!reportErrors())
            return;
        addDefaultContext();
        //Sentry.capture(e);
    }
    public static void sentryCapture(Throwable t) {
        if (!reportErrors())
            return;
        addDefaultContext();
        //Sentry.capture(t);
    }

    @Override
    public void captureException(Throwable e) {
        e.printStackTrace();
        WebAPI.sentryCapture(e);
    }

    @Override
    public Optional<String> getAssetLocation(String asset) {
        return Sponge.getAssetManager().getAsset(WebAPI.getInstance(), asset).map(a -> a.getUrl().toString());
    }

    @Override
    public Optional<byte[]> getAssetContent(String asset) {
        return Sponge.getAssetManager().getAsset(WebAPI.getInstance(), asset).flatMap(a -> {
            try {
                return Optional.of(a.readBytes());
            } catch (IOException e) {
                return Optional.empty();
            }
        });
    }
}
