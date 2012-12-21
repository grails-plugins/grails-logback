package org.slf4j.impl;

import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;

/**
 * Implementation of the StaticMarkerBinder for slf4j.
 * Based on org.slf4j.impl.StaticMarkerBinder from Grails.
 *
 * @author Graeme Rocher
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class StaticMarkerBinder {

	/**
	 * The unique instance of this class.
	 */
	public static final StaticMarkerBinder SINGLETON = new StaticMarkerBinder();

	protected IMarkerFactory markerFactory = new BasicMarkerFactory();

	private StaticMarkerBinder() {
		// singleton
	}

	/**
	 * Currently this method always returns an instance of {@link BasicMarkerFactory}.
	 */
	public IMarkerFactory getMarkerFactory() {
		return markerFactory;
	}

	/**
	 * Currently, this method returns the class name of
	 * {@link BasicMarkerFactory}.
	 */
	public String getMarkerFactoryClassStr() {
		return BasicMarkerFactory.class.getName();
	}
}
