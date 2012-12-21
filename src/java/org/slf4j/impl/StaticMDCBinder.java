package org.slf4j.impl;

import org.slf4j.spi.MDCAdapter;

import ch.qos.logback.classic.util.LogbackMDCAdapter;

/**
 * Based on org.slf4j.impl.StaticMDCBinder from Grails.
 *
 * @author Graeme Rocher
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class StaticMDCBinder {

	public static final StaticMDCBinder SINGLETON = new StaticMDCBinder();

	private StaticMDCBinder() {
		// singleton
	}

	public MDCAdapter getMDCA() {
		return new LogbackMDCAdapter();
	}

	public String getMDCAdapterClassStr() {
		return LogbackMDCAdapter.class.getName();
	}
}
