package japella;

import japella.configuration.Configuration;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.configuration.ConfigurationException;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	public static Main instance;

	public static File getConfigDir() throws Exception {
		File configDir = new File(System.getProperty("user.home"), ".japella/");

		if (!configDir.exists()) {
			if (!configDir.mkdirs()) {
				throw new Exception("Cannot make config dir: " + configDir.getAbsolutePath());
			}
		}

		return configDir;
	}

	public static void main(final String[] args) throws Exception {
		Main.instance = new Main();

		try {
			Main.instance.startup();
		} catch (Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
			Main.instance.shutdown();
		}
	}

	private final Configuration compConfig = new Configuration();

	private static final transient Logger LOG = LoggerFactory.getLogger(Main.class);
	public final ArrayList<Bot> botList = new ArrayList<Bot>();
	public final ArrayList<Server> servers = new ArrayList<Server>();

	public Instant startTime;

	public File getConfigFile() throws Exception {
		return new File(Main.getConfigDir(), "config.xml");
	}

	public Server lookupServer(final String wantedServer) throws Server.NotFoundException {
		for (final Server server : this.servers) {
			if (server.getServerName().equals(wantedServer)) {
				return server;
			}
		}

		throw new Server.NotFoundException();
	}

	public void shutdown() {
		try {
			for (Bot bot : this.botList) {
				for (MessagePlugin plugin : bot.getMessagePlugins()) {
					plugin.saveConfig();
				}

				bot.disconnect();
			}
		} catch (Exception e) {
			Main.LOG.error("Unhandled exception in the shutdown handler! " + e.toString());
			e.printStackTrace();
		}

		System.exit(0);
	}

	public void startup() throws Exception {
		Main.instance.startTime = Instant.now();

		Main.LOG.info("Japella " + Configuration.getVersion() + "\n");
		Main.LOG.debug("Configuration dir: " + Main.getConfigDir() + ", exists: " + Main.getConfigDir().exists());

		try {
			this.compConfig.parseConfigurationFile(this.getConfigFile());
		} catch (ConfigurationException e1) {
			Main.LOG.warn("Configuration error: " + e1, e1);
			return;
		}

		if (this.servers.isEmpty()) {
			Main.LOG.error("0 servers found, this program will now exit.");
			this.shutdown();
		} else {
			Main.LOG.info(this.servers.size() + " server(s) were found, oh goodie.");
		}

		if (this.botList.isEmpty()) {
			Main.LOG.error("0 Bots found, this program will now exit.");
			this.shutdown();
		} else {
			Main.LOG.info(this.botList.size() + " bot(s) were found.");
		}
	}

}
