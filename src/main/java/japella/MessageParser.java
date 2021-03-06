package japella;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageParser {
	private final String[] parts;

	private final String originalMessage;
	private String alertUsername;
	private final String actualContent;

	public MessageParser(String smessage) {
		this.originalMessage = smessage;

		Pattern p = Pattern.compile("((\\w+)[:,]).+");
		Matcher m = p.matcher(smessage);

		if (m.matches()) {
			this.alertUsername = m.group(2);
			smessage = smessage.replaceFirst(m.group(1), "");
		}

		this.actualContent = smessage.trim();
		this.parts = this.actualContent.split(" ");
	}

	public String getAlertUsername() {
		return this.alertUsername;
	}

	public String getBody(int skipWords) {
		String[] body = this.getContentBody().split(" ");

		skipWords = Math.min(skipWords, body.length);
		body = Arrays.copyOfRange(body, skipWords, body.length);

		String ret = "";

		for (String element : body) {
			ret += element + " ";
		}

		return ret.trim();
	}

	public String getContent() {
		return this.actualContent;
	}

	public String getContentBody() {
		return this.actualContent.replace(this.getKeyword(), "").trim();
	}

	public int getInt(int position) {
		String i = this.parts[position];

		if (!this.isInt(position)) {
			return 0;
		} else {
			return Integer.parseInt(i);
		}
	}

	public String getKeyword() {
		String firstWord = this.getString(0);

		if (firstWord.startsWith("!")) {
			return firstWord;
		} else {
			return "";
		}
	}

	public String getOriginalMessage() {
		return this.originalMessage;
	}

	public String getString(int position) {
		return this.parts[position];
	}

	public String getStringFirstArgument() {
		return this.getString(1).trim();
	}

	public boolean hasKeyword() {
		return this.getKeyword() != null;
	}

	public boolean hasKeyword(String keyword) {
		String mykeyword = this.getKeyword();

		return mykeyword.equalsIgnoreCase(keyword);
	}

	public boolean hasParam(int position) {
		if (position >= this.parts.length) {
			return false;
		} else {
			return true;
		}
	}

	public boolean isInt(int position) {
		if (!this.hasParam(position)) {
			return false;
		} else {
			String i = this.parts[position];

			try {
				Integer.parseInt(i);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}

	private boolean isString(int position) {
		return this.hasParam(position);
	}

	public boolean matches(Type... types) throws Exception {
		for (int i = 0; i < types.length; i++) {
			Type t = types[i];

			if (!this.hasParam(i)) {
				throw new Exception("Expected a parameter at position:" + i);
			} else {
				if (t == String.class) {
					if (this.isString(i)) {
						continue;
					} else {
						throw new Exception("Expected parameter " + i + " to be String.");
					}
				} else if (t == Integer.class) {
					if (this.isInt(i)) {
						continue;
					} else {
						throw new Exception("Expected parameter " + i + " to be int");
					}
				} else {
					throw new Exception("Parameter in position " + i + " was not a string or an int!");
				}
			}
		}

		return true;
	}

	public boolean startsWithUsername() {
		return this.alertUsername != null;
	}
}
