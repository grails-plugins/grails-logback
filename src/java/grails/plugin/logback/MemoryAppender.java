package grails.plugin.logback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.status.ErrorStatus;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class MemoryAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

	protected Encoder<ILoggingEvent> encoder;
	protected ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

	@Override
	public void start() {
		try {
			encoder.init(outputStream);
			super.start();
		}
		catch (IOException e) {
			started = false;
			addStatus(new ErrorStatus("Failed to initialize encoder for appender named [" + name + "].", this, e));
		}
	}

	@Override
	protected void append(ILoggingEvent event) {
		if (!isStarted()) {
			return;
		}

		try {
			event.prepareForDeferredProcessing();
			encoder.doEncode(event);
		}
		catch (IOException ioe) {
			started = false;
			addStatus(new ErrorStatus("IO failure in appender", this, ioe));
		}
	}

	public void setEncoder(Encoder<ILoggingEvent> e) {
		encoder = e;
	}

	public String getRenderedOutput() {
		return new String(outputStream.toByteArray());
	}
}
