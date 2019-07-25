package valandur.webapi.servlet.base;

import org.slf4j.Logger;
import valandur.webapi.WebAPI;
import valandur.webapi.config.BaseConfig;
import valandur.webapi.config.IServletsConfig;
import valandur.webapi.integration.activetime.ActiveTimeServlet;
import valandur.webapi.integration.cmdscheduler.CmdSchedulerServlet;
import valandur.webapi.integration.gwmcrates.GWMCratesServlet;
import valandur.webapi.integration.mmcrestrict.MMCRestrictServlet;
import valandur.webapi.integration.mmctickets.MMCTicketsServlet;
import valandur.webapi.integration.nucleus.NucleusServlet;
import valandur.webapi.integration.redprotect.RedProtectServlet;
import valandur.webapi.integration.universalmarket.UniversalMarketServlet;
import valandur.webapi.integration.villagershops.VShopServlet;
import valandur.webapi.integration.webbooks.WebBooksServlet;
import valandur.webapi.servlet.*;

import javax.ws.rs.Path;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static valandur.webapi.Constants.*;

/**
 * This service allows registering servlets with the Web-API, which it will serve for clients.
 * Your servlet must inherit from {@link BaseServlet} and have the
 * {@link javax.ws.rs.Path} annotation specifying the base path at which the servlet will
 * be accessible.
 */
public class ServletService {
    private static final String configFileName = "servlets.conf";
    private static final int maxNameWidth = 15; // Length of "UniversalMarket"
    private static final String STATUS_ON = "ON";
    private static final String STATUS_OFF = "DISABLED";
    private static final String STATUS_NOTFOUND = "NOT FOUND";

    private Logger logger;

    private Map<String, Class<? extends BaseServlet>> servletClasses = new HashMap<>();


    public void init() {
        this.logger = WebAPI.getInstance().getLogger();

        logger.info("Registering servlets...");

        java.nio.file.Path configPath = WebAPI.getConfigPath().resolve(configFileName).normalize();
        IServletsConfig config = BaseConfig.load(configPath, new IServletsConfig());

        servletClasses.clear();

        if (config.Block) registerServlet(BlockServlet.class);
        if (config.Chunk) registerServlet(ChunkServlet.class);
        if (config.Cmd) registerServlet(CmdServlet.class);
        if (config.Economy) registerServlet(EconomyServlet.class);
        if (config.Entity) registerServlet(EntityServlet.class);
        if (config.History) registerServlet(HistoryServlet.class);
        if (config.Info) registerServlet(InfoServlet.class);
        if (config.InteractiveMessage) registerServlet(InteractiveMessageServlet.class);
        if (config.Map) registerServlet(MapServlet.class);
        if (config.Permission) registerServlet(PermissionServlet.class);
        if (config.Player) registerServlet(PlayerServlet.class);
        if (config.Plugin) registerServlet(PluginServlet.class);
        if (config.Recipe) registerServlet(RecipeServlet.class);
        if (config.Registry) registerServlet(RegistryServlet.class);
        if (config.Server) registerServlet(ServerServlet.class);
        if (config.TileEntity) registerServlet(TileEntityServlet.class);
        if (config.User) registerServlet(UserServlet.class);
        if (config.World) registerServlet(WorldServlet.class);

        // Other plugin integrations
        if (config.integrations.ActiveTime) {
            try {
                Class.forName("com.mcsimonflash.sponge.activetime.ActiveTime");
                registerServlet(ActiveTimeServlet.class);
                this.logServletStatus("ActiveTime", STATUS_ON);
            } catch (ClassNotFoundException ignored) {
                this.logServletStatus("ActiveTime", STATUS_NOTFOUND);
            }
        } else {
            this.logServletStatus("ActiveTime", STATUS_OFF);
        }

        if (config.integrations.CmdScheduler) {
            try {
                Class.forName("com.mcsimonflash.sponge.cmdscheduler.CmdScheduler");
                registerServlet(CmdSchedulerServlet.class);
                this.logServletStatus("CmdScheduler", STATUS_ON);
            } catch (ClassNotFoundException ignored) {
                this.logServletStatus("CmdScheduler", STATUS_NOTFOUND);
            }
        } else {
            this.logServletStatus("CmdScheduler", STATUS_OFF);
        }

        if (config.integrations.GWMCrates) {
            try {
                Class.forName("org.gwmdevelopments.sponge_plugin.crates.GWMCrates");
                registerServlet(GWMCratesServlet.class);
                this.logServletStatus("GWMCrates", STATUS_ON);
            } catch (ClassNotFoundException ignored) {
                this.logServletStatus("GWMCrates", STATUS_NOTFOUND);
            }
        } else {
            this.logServletStatus("GWMCrates", STATUS_OFF);
        }

        /*if (config.integrations.HuskyCrates) {
            try {
                Class.forName("com.codehusky.huskycrates.HuskyCrates");
                registerServlet(HuskyCratesServlet.class);
                this.logServletStatus("HuskyCrates", STATUS_ON);
            } catch (ClassNotFoundException ignored) {
                this.logServletStatus("HuskyCrates", STATUS_NOTFOUND);
            }
        } else {
            this.logServletStatus("HuskyCrates", STATUS_OFF);
        }*/

        if (config.integrations.MMCRestrict) {
            try {
                Class.forName("net.moddedminecraft.mmcrestrict.Main");
                registerServlet(MMCRestrictServlet.class);
                this.logServletStatus("MMCRestrict", STATUS_ON);
            } catch (ClassNotFoundException ignored) {
                this.logServletStatus("MMCRestrict", STATUS_NOTFOUND);
            }
        } else {
            this.logServletStatus("MMCRestrict", STATUS_OFF);
        }

        if (config.integrations.MMCTickets) {
            try {
                Class.forName("net.moddedminecraft.mmctickets.Main");
                registerServlet(MMCTicketsServlet.class);
                this.logServletStatus("MMCRestrict", STATUS_ON);
            } catch (ClassNotFoundException ignored) {
                this.logServletStatus("MMCRestrict", STATUS_NOTFOUND);
            }
        } else {
            this.logServletStatus("MMCRestrict", STATUS_OFF);
        }

        if (config.integrations.Nucleus) {
            try {
                Class.forName("io.github.nucleuspowered.nucleus.api.NucleusAPI");
                registerServlet(NucleusServlet.class);
                this.logServletStatus("Nucleus", STATUS_ON);
            } catch (ClassNotFoundException ignored) {
                this.logServletStatus("Nucleus", STATUS_NOTFOUND);
            }
        } else {
            this.logServletStatus("Nucleus", STATUS_OFF);
        }

        if (config.integrations.RedProtect) {
            try {
                Class.forName("br.net.fabiozumbi12.RedProtect.Sponge.RedProtect");
                registerServlet(RedProtectServlet.class);
                this.logServletStatus("RedProtect", STATUS_ON);
            } catch (ClassNotFoundException ignored) {
                this.logServletStatus("RedProtect", STATUS_NOTFOUND);
            }
        } else {
            this.logServletStatus("RedProtect", STATUS_OFF);
        }

        if (config.integrations.UniversalMarket) {
            try {
                Class.forName("com.xwaffle.universalmarket.UniversalMarket");
                registerServlet(UniversalMarketServlet.class);
                this.logServletStatus("UniversalMarket", STATUS_ON);
            } catch (ClassNotFoundException ignored) {
                this.logServletStatus("UniversalMarket", STATUS_NOTFOUND);
            }
        } else {
            this.logServletStatus("UniversalMarket", STATUS_OFF);
        }

        if (config.integrations.VillagerShops) {
            try {
                Class.forName("de.dosmike.sponge.vshop.VillagerShops");
                registerServlet(VShopServlet.class);
                this.logServletStatus("VillagerShops", STATUS_ON);
            } catch (ClassNotFoundException ignored) {
                this.logServletStatus("VillagerShops", STATUS_NOTFOUND);
            }
        } else {
            this.logServletStatus("VillagerShops", STATUS_OFF);
        }

        if (config.integrations.WebBooks) {
            try {
                Class.forName("de.dosmike.sponge.WebBooks.WebBooks");
                registerServlet(WebBooksServlet.class);
                this.logServletStatus("WebBooks", STATUS_ON);
            } catch (ClassNotFoundException ignored) {
                this.logServletStatus("WebBooks", STATUS_NOTFOUND);
            }
        } else {
            this.logServletStatus("WebBooks", STATUS_OFF);
        }
    }

    private void logServletStatus(String servletName, String status) {
        String separator = String.join("", Collections.nCopies(maxNameWidth - servletName.length(), " "));
        String color = status.equals(STATUS_ON) ? ANSI_GREEN : status.equals(STATUS_OFF) ? ANSI_RED : ANSI_BLUE;
        logger.info("  " + servletName + separator + " [" + color + status + ANSI_RESET + "]");
    }

    /**
     * Register a servlet with the WebAPI, which will give it a separate base address
     * @param servlet The class of servlet to register. The WebAPI will create an instance when starting. Make
     *                sure to provide an empty constructor.
     */
    public void registerServlet(Class<? extends BaseServlet> servlet) {
        if (!servlet.isAnnotationPresent(Path.class)) {
            logger.error("Servlet " + servlet.getName() + " is missing @Path annotation");
            return;
        }

        Path info = servlet.getAnnotation(Path.class);
        String basePath = info.value();
        if (basePath.endsWith("/"))
            basePath = basePath.substring(0, basePath.length() - 1);
        if (!basePath.startsWith("/"))
            basePath = "/" + basePath;

        try {
            Method m = servlet.getMethod("onRegister");
            m.invoke(null);
        } catch (NoSuchMethodException ignored) {
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            WebAPI.sentryCapture(e);
        }

        servletClasses.put(basePath, servlet);
    }

    /**
     * Gets a map of all available base paths mapped to the servlets that implement them.
     * @return A map from base path to implementing servlet class.
     */
    public Map<String, Class<? extends BaseServlet>> getRegisteredServlets() {
        return servletClasses;
    }
}
