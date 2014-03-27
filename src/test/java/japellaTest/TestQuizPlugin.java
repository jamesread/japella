package japellaTest;

import japella.Bot;
import japella.MessageParser;
import japella.MessagePlugin.Message;
import japella.messagePlugins.QuizPlugin;

import org.junit.Assert;
import org.junit.Test;

public class TestQuizPlugin {
	@Test
	public void testAddBadQuizQuestion() {
		QuizPlugin quizPlugin = new QuizPlugin();

		Bot bot = new Bot("QuizBot", null);
		bot.loadMessagePlugin(quizPlugin);

		Assert.assertEquals(0, quizPlugin.getQuestions().size());

		bot.onMockMessage(new Message(bot, "#channel", "auser", new MessageParser("!addquizquestion foo!")));

		Assert.assertEquals(0, quizPlugin.getQuestions().size());

	}

	@Test
	public void testAddQuizQuestion() {
		QuizPlugin quizPlugin = new QuizPlugin();

		Bot bot = new Bot("QuizBot", null);
		bot.loadMessagePlugin(quizPlugin);

		Assert.assertEquals(0, quizPlugin.getQuestions().size());

		bot.onMockMessage(new Message(bot, "#channel", "auser", new MessageParser("!addquizquestion foo?=bar!")));

		Assert.assertEquals(1, quizPlugin.getQuestions().size());

		Message messageQuizQuestionCount = bot.onMockMessage(new Message(bot, "#channel", "auser", new MessageParser("!quizquestioncount")));

		Assert.assertEquals("The quiz has 1 in the database.", messageQuizQuestionCount.replies.firstElement());
	}
}
