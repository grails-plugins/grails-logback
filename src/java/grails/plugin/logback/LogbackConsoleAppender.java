package grails.plugin.logback;

import grails.build.logging.GrailsConsole;
import groovy.util.ConfigObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.codehaus.groovy.grails.cli.logging.GrailsConsolePrintStream;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.exceptions.DefaultStackTraceFilterer;
import org.codehaus.groovy.grails.exceptions.DefaultStackTracePrinter;
import org.codehaus.groovy.grails.exceptions.StackTraceFilterer;
import org.codehaus.groovy.grails.exceptions.StackTracePrinter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.pattern.PatternLayoutBase;

/**
 * Appends to the GrailsConsole instance in dev mode.
 *
 * Based on org.codehaus.groovy.grails.plugins.log4j.appenders.GrailsConsoleAppender.
 *
 * @author Graeme Rocher
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class LogbackConsoleAppender extends AppenderBase<ILoggingEvent> {

	protected static final String MESSAGE_PREFIX = "Message: ";
	protected GrailsConsole console = GrailsConsole.getInstance();
	protected StackTracePrinter stackTracePrinter;
	protected StackTraceFilterer stackTraceFilterer;
	protected PatternLayoutEncoder encoder = new PatternLayoutEncoder();
	protected ByteArrayOutputStream os = new ByteArrayOutputStream();

	public LogbackConsoleAppender(ConfigObject config) {
		createStackTracePrinter(config);
		createStackTraceFilterer(config);
	}

	public void setLayout(Layout<ILoggingEvent> layout) {
		if (layout instanceof PatternLayoutBase) {
			encoder.setPattern(((PatternLayoutBase<?>)layout).getPattern());
		}
	}

	@Override
	public void start() {
		encoder.setContext(getContext());
		encoder.start();
		try {
			encoder.init(os);
		}
		catch (IOException ignored) {
			// can't happen with ByteArrayOutputStream
		}
		super.start();
	}

	@Override
	protected void append(ILoggingEvent event) {
		Level level = event.getLevel();
		String message = buildMessage(event);
		if (System.out instanceof GrailsConsolePrintStream) {
			if (level.isGreaterOrEqual(Level.ERROR)) {
				console.error(message);
			}
			else {
				console.log(message);
			}
		}
		else {
			if (level.isGreaterOrEqual(Level.ERROR)) {
				System.err.println(message);
			}
			else {
				System.out.println(message);
			}
		}
	}

	protected String buildMessage(ILoggingEvent event) {

		StringBuilder sbuf = new StringBuilder();

		StringBuilder b = new StringBuilder(format(event));
		if (console.isVerbose()) {
			IThrowableProxy tp = event.getThrowableProxy();
			if (tp != null) {
				StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
				int commonFrames = tp.getCommonFrames();
				b.append(CoreConstants.LINE_SEPARATOR);
				for (int i = 0; i < stepArray.length - commonFrames; i++) {
					StackTraceElementProxy step = stepArray[i];
					b.append(step.toString()).append(CoreConstants.LINE_SEPARATOR);
				}
				if (commonFrames > 0) {
//					sbuf.append(TRACE_PREFIX);
					sbuf.append("\t... " + commonFrames).append(" common frames omitted").append(CoreConstants.LINE_SEPARATOR);
				}
			}
		}
		else {
			if (event.getThrowableProxy() instanceof ThrowableProxy) {
				Throwable throwable = ((ThrowableProxy)event.getThrowableProxy()).getThrowable();
				if (throwable != null) {
					b.append(MESSAGE_PREFIX).append(throwable.getMessage()).append(CoreConstants.LINE_SEPARATOR);
					stackTraceFilterer.filter(throwable, true);
					b.append(stackTracePrinter.prettyPrint(throwable));
				}
			}
		}

		return b.toString();
	}

	protected synchronized String format(ILoggingEvent event) {
		try {
			encoder.doEncode(event);
		}
		catch (IOException ignored) {
			// can't happen with ByteArrayOutputStream
		}

		String formatted = new String(os.toByteArray());
		os.reset();

		return formatted;
	}

	protected void createStackTracePrinter(ConfigObject config) {
		try {
			stackTracePrinter = (StackTracePrinter)GrailsClassUtils.instantiateFromConfig(
					config, "grails.logging.stackTracePrinterClass", DefaultStackTracePrinter.class.getName());
		}
		catch (Throwable t) {
			addWarn("Problem instantiating StackTracePrinter class, using default: " + t.getMessage());
			stackTracePrinter = new DefaultStackTracePrinter();
		}
	}

	protected void createStackTraceFilterer(ConfigObject config) {
		try {
			stackTraceFilterer = (StackTraceFilterer)GrailsClassUtils.instantiateFromConfig(
					config, "grails.logging.stackTraceFiltererClass", DefaultStackTraceFilterer.class.getName());
		}
		catch (Throwable t) {
			addError("Problem instantiating StackTraceFilter class, using default: " + t.getMessage());
			stackTraceFilterer = new DefaultStackTraceFilterer();
		}
	}
}
