package grails.plugin.logback;

import ch.qos.logback.classic.html.DefaultCssBuilder;
import ch.qos.logback.classic.html.DefaultThrowableRenderer;
import ch.qos.logback.classic.html.HTMLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.html.CssBuilder;
import ch.qos.logback.core.html.IThrowableRenderer;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class HtmlEncoder extends LayoutWrappingEncoder<ILoggingEvent> {

	protected CssBuilder cssBuilder = new DefaultCssBuilder();
	protected String pattern;
	protected String title;
	protected IThrowableRenderer<ILoggingEvent> throwableRenderer = new DefaultThrowableRenderer();

	public void setCssBuilder(CssBuilder b) {
		cssBuilder = b;
	}

	public void setPattern(String p) {
		pattern = p;
	}

	public void setTitle(String t) {
		title = t;
	}

	public void setThrowableRenderer(IThrowableRenderer<ILoggingEvent> t) {
		throwableRenderer = t;
	}

	@Override
	public void start() {
		HTMLLayout htmlLayout = new HTMLLayout();
		htmlLayout.setContext(context);
		htmlLayout.setCssBuilder(cssBuilder);
		htmlLayout.setPattern(pattern);
		htmlLayout.setTitle(title);
		htmlLayout.setThrowableRenderer(throwableRenderer);
		htmlLayout.start();
		layout = htmlLayout;
		super.start();
	}
}
