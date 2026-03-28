package dev.jackwith.skyCoreV2.utils;
import dev.jackwith.skyCoreV2.SkyCore;

import java.util.logging.Logger;

public class StackTrace {

    private static final Logger logger = SkyCore.getInstance().getLogger();

    public static void error(String context, Exception e) {
        logger.severe("§8[§cSkyCore§8] §cAn error occurred" + (context != null ? " (" + context + ")" : ""));

        if (e == null) {
            logger.severe("§8» §7No exception provided.");
            return;
        }

        logger.severe("§8» §c" + e.getClass().getSimpleName() + ": §7" + e.getMessage());

        StackTraceElement[] stack = e.getStackTrace();
        int limit = Math.min(stack.length, 6);

        for (int i = 0; i < limit; i++) {
            StackTraceElement el = stack[i];
            logger.severe("§8» §7at §f" + el.getClassName() + "." + el.getMethodName()
                    + "§8(§7" + el.getFileName() + ":" + el.getLineNumber() + "§8)");
        }

        if (stack.length > limit) {
            logger.severe("§8» §7... (" + (stack.length - limit) + " more)");
        }
    }

    public static void warn(String message) {
        logger.warning("§8[§eSkyCore§8] §e" + message);
    }

    public static void info(String message) {
        logger.info("§8[§bSkyCore§8] §7" + message);
    }
}