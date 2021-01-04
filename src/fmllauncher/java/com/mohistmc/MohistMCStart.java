package com.mohistmc;

import com.mohistmc.config.MohistConfigUtil;
import com.mohistmc.libraries.CustomLibraries;
import com.mohistmc.libraries.DefaultLibraries;
import com.mohistmc.network.download.DownloadJava;
import com.mohistmc.network.download.UpdateUtils;
import static com.mohistmc.util.EulaUtil.hasAcceptedEULA;
import static com.mohistmc.util.EulaUtil.writeInfos;
import com.mohistmc.util.InstallUtils;
import static com.mohistmc.util.InstallUtils.startInstallation;
import com.mohistmc.util.JarLoader;
import com.mohistmc.util.i18n.i18n;

import java.util.Scanner;
import static net.minecraftforge.server.ServerMain.startMohistServer;

public class MohistMCStart {

    public static String getVersion() {
        return (MohistMCStart.class.getPackage().getImplementationVersion() != null) ? MohistMCStart.class.getPackage().getImplementationVersion() : "unknown";
    }

    public static String getForgeVersion() {
        return (MohistMCStart.class.getPackage().getSpecificationVersion() != null) ? MohistMCStart.class.getPackage().getSpecificationVersion() : "unknown";
    }

    public static String getMCPVersion() {
        return (MohistMCStart.class.getPackage().getSpecificationTitle() != null) ? MohistMCStart.class.getPackage().getSpecificationTitle() : "unknown";
    }

    public static void main(String[] args) throws Exception {
        float javaVersion = Float.parseFloat(System.getProperty("java.class.version"));
        MohistConfigUtil.copyMohistConfig();

        if (javaVersion > 54.0 && MohistConfigUtil.bMohist("use_custom_java8", "false"))
            DownloadJava.run(args);

        if (MohistConfigUtil.bMohist("show_logo", "true"))
            System.out.println("\n" + "\n" +
                    " __    __   ______   __  __   __   ______   ______  \n" +
                    "/\\ \"-./  \\ /\\  __ \\ /\\ \\_\\ \\ /\\ \\ /\\  ___\\ /\\__  _\\ \n" +
                    "\\ \\ \\-./\\ \\\\ \\ \\/\\ \\\\ \\  __ \\\\ \\ \\\\ \\___  \\\\/_/\\ \\/ \n" +
                    " \\ \\_\\ \\ \\_\\\\ \\_____\\\\ \\_\\ \\_\\\\ \\_\\\\/\\_____\\  \\ \\_\\ \n" +
                    "  \\/_/  \\/_/ \\/_____/ \\/_/\\/_/ \\/_/ \\/_____/   \\/_/ \n" +
                    "                                                    \n" + "\n" +
                    "                                      " + i18n.get("mohist.launch.welcomemessage"));
        if (MohistConfigUtil.bMohist("check_libraries", "true")) {
            DefaultLibraries.run();
            startInstallation();
        }
        CustomLibraries.loadCustomLibs();
        new JarLoader().loadJar(InstallUtils.extra);
        if (MohistConfigUtil.bMohist("check_update", "true")) UpdateUtils.versionCheck();
        if (!hasAcceptedEULA()) {
            System.out.println(i18n.get("eula"));
            while (!"true".equals(new Scanner(System.in).next())) ;
            writeInfos();
        }
        startMohistServer(args);
    }
}