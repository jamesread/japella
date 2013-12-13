package japella;

import japella.messagePlugins.Decide;
import japella.messagePlugins.GaggingPlugin;
import japella.messagePlugins.KarmaTracker;
import japella.messagePlugins.QuizPlugin;
import japella.messagePlugins.TicketLookup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.ReplyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bot extends PircBot implements Runnable {
	private String nick = null;
	private Server server = null;
	private boolean operator = false;
	private final Vector<String> channels = new Vector<String>();
	private String password = "supersecret";
	private final Vector<String> admins = new Vector<String>();
	private boolean discrete = true;

	@Deprecated
	private Main mainReference = Main.instance;
	private String ownerNickname = "unknownOwner";
	private final ArrayList<MessagePlugin> messagePlugins = new ArrayList<MessagePlugin>();

	private final Thread runner;

	private String lastWhois = "";

	private static final transient Logger LOG = LoggerFactory.getLogger(Bot.class);

	private final Hashtable<String, String> cachedWhoisQueries = new Hashtable<String, String>();

	private File watchDirectory;

	private final Vector<String> channelGags = new Vector<String>();

	/**
	 * The constructor.
	 * 
	 * @param nick
	 *            The nickname that the bot will use on the irc server.
	 * @param server
	 *            A string representing the server address. IP addresses or
	 *            domain names are acceptable.
	 * @param channels
	 *            An array list of channels for the bot to connect to. Channels
	 *            require the additional hash (#) before the channel name.
	 * @param mainReference
	 *            A reference to the main class. This is so that Main can shut
	 *            the bots down and perform additional tasks.
	 */
	public Bot(final String nick, final Server server) {
		this.nick = nick;
		this.setVerbose(true);

		this.debugMessage("Constructing bot: " + this.nick + " with password: " + this.password);

		this.setName(this.nick);
		this.setVersion("Japella " + this.mainReference.getConfiguration().getVersion());
		this.setFinger("Get your fingers off me!");
		this.setLogin(this.nick);

		// this.realname = "Japella " + this.mainReference.VERISON;
		this.server = server;

		this.mainReference = Main.instance;

		this.loadMessagePlugins();

		this.runner = new Thread(this, "Bot: " + this.nick);
	}

	public void addChannel(String channel) {
		this.channels.add(channel);
	}

	public void addChannels(Vector<String> channels) {
		this.channels.addAll(channels);
	}

	/**
	 * A wrapper for connect in pircbot. Simply displays the status message too.
	 */
	private void connect() {
		try {
			this.debugMessage("Attempting to connect to server \"" + this.server.getServerName() + "\".\n");
			this.connect(this.server.getAddress(), this.server.getPort());
			this.debugMessage("Connected to \"" + this.server.getServerName() + "\"");

			this.joinAllChannels();
		} catch (final NickAlreadyInUseException e) {
			this.debugMessage("Nickname already in use on \"" + this.server.getServerName() + "\".\n");

			this.disconnect(); // superclass
		} catch (final Exception e) {
			this.debugMessage("Cannot connect: " + e.toString() + "\n");
		}
	}

	public void debugMessage(final String message) {
		Bot.LOG.debug(this.nick + ": " + message.trim());
	}

	public MessagePlugin getMessagePlugin(String pluginName) {
		for (MessagePlugin mp : this.messagePlugins) {
			if (mp.getClass().getSimpleName().equals(pluginName)) {
				return mp;
			}
		}

		return null;
	}

	public File getWatchDirectory() {
		return this.watchDirectory;
	}

	public String getWhois(final String username) {
		if (this.cachedWhoisQueries.containsKey(username)) {
			return this.cachedWhoisQueries.get(username);
		}

		this.debugMessage("Got reply: " + this.lastWhois);
		this.cachedWhoisQueries.put(username, this.lastWhois);

		return this.lastWhois;
	}

	public boolean hasAdmin(String sender) {
		return this.admins.contains(sender);
	}

	/**
	 * Join all the channels within this bot's channel list.
	 */
	private void joinAllChannels() {
		this.debugMessage("Going to join " + this.channels.size() + " channels.");

		String currentChannel = "";

		for (int i = 0; i < this.channels.size(); i++) {
			currentChannel = this.channels.get(i);

			this.joinChannel(currentChannel);
		}

	}

	private void loadMessagePlugins() {
		this.messagePlugins.add(new KarmaTracker());
		this.messagePlugins.add(new Decide());
		this.messagePlugins.add(new TicketLookup());
		this.messagePlugins.add(new GaggingPlugin());
		this.messagePlugins.add(new QuizPlugin());
	}

	private void onAnyMessage(final String channel, final String sender, final String message) {}

	/**
	 * Called by the superclass when the bot is deop'd.
	 */
	@Override
	public void onDeop(final String channel, final String sourceNick, final String sourceLogin, final String sourceHostname, final String recipient) {
		if (recipient.matches(this.nick)) {
			this.debugMessage("De-opped in \"" + channel + "\" by \"" + sourceNick + "\".\n");

			this.operator = false;
		}
	}

	/**
	 * Called by the superclass when somebody joins the same channel as us.
	 */
	@Override
	public void onJoin(final String channel, final String sender, final String login, final String hostname) {
		if (this.nick.equals(sender)) {
			this.debugMessage("I have joined \"" + channel + "\".");
		} else {
			this.debugMessage("\"" + sender + "\" joined \"" + channel + "\".");
		}

		this.setUserModes(channel, sender, hostname);
	}

	/**
	 * Called by the superclass when this bot recieves a message.
	 */
	@Override
	public void onMessage(final String channel, final String sender, final String login, final String hostname, final String message) {
		this.onAnyMessage(channel, sender, message);

		for (MessagePlugin mp : this.messagePlugins) {
			mp.onMessage(this, channel, sender, login, hostname, message);
		}
	}

	/**
	 * Called by the superclass when the bot is op'ed.
	 */
	@Override
	public void onOp(final String channel, final String sourceNick, final String sourceLogin, final String sourceHostname, final String recipient) {
		if (recipient.matches(this.nick)) {
			this.debugMessage("Op'ed in \"" + channel + "\" by \"" + sourceNick + "\".");
			this.operator = true;
		}
	}

	/**
	 * Called by the superclass when the bot recieves a private message.
	 */
	@Override
	public void onPrivateMessage(final String sender, final String login, final String hostname, final String message) {
		Bot.LOG.debug("recv PM: " + sender + ": " + message);

		this.onAnyMessage(sender, sender, message);

		if (message.contains("!join")) {
			String channel = message.replace("!join", "").trim();

			if (this.admins.contains(sender)) {
				this.joinChannel(channel);
			} else {
				String reply = "You cannot tell me to join a channel (" + channel + "), because you are not an admin";

				this.sendMessageResponsibly(sender, reply);
				this.log(reply);
			}
		} else if (message.startsWith("!discrete")) {
			this.discrete = !this.discrete;
			this.sendMessageResponsibly(sender, "Discression is now: " + this.discrete);
		} else if (message.startsWith("!channels")) {
			String reply = "";

			for (String channel : this.getChannels()) {
				reply += channel + " ";
			}

			this.sendMessageResponsibly(sender, reply);
			this.log(reply);
		} else if (message.contains("!part")) {
			String channel = message.replace("!part", "").trim();

			if (this.admins.contains(sender)) {
				this.partChannel(channel);
			} else {
				String reply = "You cannot tell me to leave a channel (" + channel + "), because you are not an admin";

				this.sendMessageResponsibly(sender, reply);
				this.log(reply);
			}
		} else if (message.startsWith("!quit")) {
			if (this.admins.contains(sender)) {
				System.exit(0);
			} else {
				this.sendMessageResponsibly(sender, "You are not an admin. Won't quit.");
			}
		} else if (message.equals("!whoami")) {
			String whois = this.getWhois(sender);
			this.sendMessageResponsibly(sender, "Thanks, your whois is: " + whois);
		} else if (message.contains("!password")) {
			final String password = message.replace("!password ", "");

			final String whoisLine = this.getWhois(sender);

			if (whoisLine == null) {
				this.sendMessageResponsibly(sender, "Cannot get your WHOIS details. Did you do a !whoami ?");
				this.debugMessage("Cannot get WHOIS details for " + sender);

				return;
			}

			if (password.trim().equals(this.password)) {
				this.sendMessageResponsibly(sender, "Password accepted.");
				this.debugMessage("Administrative password accepted from " + sender);
				this.admins.add(sender);
			} else {
				this.sendMessageResponsibly(sender, "Password rejected");
				this.debugMessage("Administrative password rejected from " + sender + ". They provided: " + password);
			}
		} else if (message.equalsIgnoreCase("!help")) {
			this.sendMessageResponsibly(sender, "Hello there. I am a channel bot (Software: Japella, version " + this.mainReference.getConfiguration().getVersion() + ")");
			this.sendMessageResponsibly(sender, "I am owned by \"" + this.ownerNickname + "\". Please PM \"" + this.ownerNickname + "\" if you are having problems this bot. ");
		} else if (message.equalsIgnoreCase("!plugins")) {
			StringBuilder buf = new StringBuilder("Plugins: ");

			for (MessagePlugin mp : this.messagePlugins) {
				buf.append(mp.getClass().getSimpleName() + ". ");
			}

			this.sendMessageResponsibly(sender, buf.toString());
		} else if (message.equals("!admins")) {
			this.sendMessageResponsibly(sender, "Admins: " + this.admins.toString());
		} else if (message.equals("!version")) {
			this.sendMessageResponsibly(sender, "Version: " + this.getVersion());
		} else {
			for (MessagePlugin mp : this.messagePlugins) {
				mp.onPrivateMessage(this, sender, message);
			}
		}
	}

	@Override
	protected void onServerResponse(final int code, final String response) {
		if (code == ReplyConstants.RPL_WHOISUSER) {
			this.debugMessage("Got WHOIS info back from server.");

			final String parts[] = response.split(" ");

			this.lastWhois = parts[1].toLowerCase() + ":" + parts[2] + "@" + parts[3];
		}
	}

	@Override
	protected void onUnknown(final String line) {
		this.debugMessage("Unknown: " + line);
	}

	/**
	 * Attempts to connect to the specified server. Will loop in a seperate
	 * thread until the bot is disconnected. Loops once a seccond.
	 */
	@Override
	public void run() {
		this.connect();

		while (this.isConnected()) {
			try {
				Thread.sleep(1000); // 1 sec
			} catch (final Exception e) {
				System.out.print("Cannot sleep: " + e.toString() + "\n");
				break;
			}
		}

		this.debugMessage("Disconnected from \"" + this.server.getServerName() + "\". \n");
	}

	public void sendMessageResponsibly(String target, String message) {
		if (this.channelGags.contains(target)) {
			Bot.LOG.info("Gagged, wont send - " + target + ": " + message);
		} else {
			Bot.LOG.info("Sending - " + target + ": " + message);
			this.sendMessage(target, message);
		}
	}

	public void sendMessageResponsiblyUser(String channel, String sender, String message) {
		this.sendMessageResponsibly(channel, sender + ": " + message.trim());
	}

	public void sendWhois(final String username) {
		// this.sendRawLineViaQueue("WHOIS " + username);
	}

	public void setOwnerNickname(String newNickname) {
		if (newNickname == null) {
			return;
		}

		this.ownerNickname = newNickname;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * After somebody joins a chanel, setUserModes is called for that user. If
	 * the bot is not an op, it exits. If the user is found in ops.lst, the user
	 * is opped, if not, the user is voiced.
	 */
	private void setUserModes(final String channel, final String sender, final String hostname) {
		if (this.nick.equals(sender)) {
			return;
		}

		if (!this.operator) {
			return;
		}

		try {
			File channelOpsFile = new File("servers/" + this.server.getServerName() + "/channels/" + channel + "/ops.lst");

			if (channelOpsFile.exists()) {
				final BufferedReader in = new BufferedReader(new FileReader(channelOpsFile));

				String line = "";

				while ((line = in.readLine()) != null) {
					if (hostname.trim().matches(line)) {
						this.debugMessage("Op'ing: \"" + sender + "\", host: \"" + hostname + "\"\n");

						this.op(channel, sender);
					} else {
						this.debugMessage("Voicing: \"" + sender + "\", host: \"" + hostname + "\"\n");

						this.voice(channel, sender);
					}
				}

				in.close();
			} else {
				this.debugMessage("Voicing: \"" + sender + "\", host: \"" + hostname + "\"\n");

				this.voice(channel, sender);
			}
		} catch (final Exception e) {
			this.debugMessage("Could not open this channel's op file: " + e.toString());
			return;
		}
	}

	public void setWatchDirectory(File watchDirectory) {
		this.watchDirectory = watchDirectory;
	}

	public void start() {
		this.runner.start();
	}

	public void toggleChannelGag(String channel) {
		if (this.channelGags.contains(channel)) {
			this.channelGags.remove(channel);

			this.log("Channel gag removed on channel:" + channel);
			this.sendMessage(channel, "Oh, I just woke up. I will talk to this channel again.");
		} else {
			this.channelGags.add(channel);

			this.log("Channel gag added on channel: " + channel);
			this.sendMessage(channel, "I will no longer send messages to this channel.");
		}
	}
}