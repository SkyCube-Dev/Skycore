package dev.jackwith.skyCoreV2.utils;

public class TimeF {
    public static String formatTime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder time = new StringBuilder();

        if (days > 0) {
            time.append(days).append("d ");
        }
        if (hours > 0) {
            time.append(hours).append("h ");
        }
        if (minutes > 0) {
            time.append(minutes).append("m ");
        }
        if (seconds > 0 || time.isEmpty()) {
            time.append(seconds).append("s");
        }

        return time.toString().trim();
    }
}
