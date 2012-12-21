package grails.plugin.logback;

import ch.qos.logback.classic.log4j.XMLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class XmlEncoder extends LayoutWrappingEncoder<ILoggingEvent> {

	protected boolean locationInfo;
	protected boolean properties;

	public void setLocationInfo(boolean b) {
		locationInfo = b;
	}

	public void setProperties(boolean b) {
		properties = b;
	}

	@Override
	public void start() {
		XMLLayout xmlLayout = new XMLLayout();
		xmlLayout.setContext(context);
		xmlLayout.setLocationInfo(locationInfo);
		xmlLayout.setProperties(properties);

		xmlLayout.start();
		layout = xmlLayout;
		super.start();
	}
}
