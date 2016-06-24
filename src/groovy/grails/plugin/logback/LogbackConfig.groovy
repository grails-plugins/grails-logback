package grails.plugin.logback

import grails.util.BuildSettingsHolder
import grails.util.Environment

import org.apache.commons.beanutils.BeanUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.db.DBAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.Appender
import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.encoder.EchoEncoder
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.helpers.NOPAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.spi.ContextAware
import ch.qos.logback.core.spi.LifeCycle

/**
 * Encapsulates the configuration of Logback. Based on org.codehaus.groovy.grails.plugins.log4j.Log4jConfig.
 *
 * @author Graeme Rocher
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class LogbackConfig {

	protected static final ENCODERS = [xml: XmlEncoder, html: HtmlEncoder, simple: EchoEncoder, pattern: PatternLayoutEncoder]
	protected static final APPENDERS = [jdbc: DBAppender, 'null': NOPAppender, console: ConsoleAppender,
	                                    file: FileAppender, rollingFile: RollingFileAppender, async: AsyncAppender]

	protected Map appenders = [:]
	protected ConfigObject config
	protected List created = []
	protected LoggerContext context = LoggerFactory.getILoggerFactory()

	protected static final String DEFAULT_ENCODER_PATTERN = '%d [%t] %-5p %c{2} %X - %m%n'

	LogbackConfig(ConfigObject config) {
		this.config = config
	}

	void reset() {
		context.reset()
		context.statusManager.add new GrailsLogbackStatusListener()
	}

	static void initialize(ConfigObject config) {
		if (config == null) {
			return
		}

		Object logback = config.logback
		LogbackConfig logbackConfig = new LogbackConfig(config)
		logbackConfig.reset()

		if (logback instanceof Closure) {
			logbackConfig.configure((Closure<?>)logback)
		}
		else if (logback instanceof Map) {
			logbackConfig.configure((Map<?, ?>)logback)
		}
		else if (logback instanceof Collection) {
			logbackConfig.configure((Collection<?>)logback)
		}
		else {
			// setup default logging
			logbackConfig.configure()
		}

		for (thing in logbackConfig.created) {
			logbackConfig.init thing
		}
	}

	def propertyMissing(String name) {
		if (ENCODERS.containsKey(name)) {
			Encoder encoder = ENCODERS[name].newInstance()
			created << encoder
			return encoder
		}

		LogLog.error "Property missing when configuring Logback: $name"

		throw new MissingPropertyException("Property missing when configuring Logback: $name")
	}

	def methodMissing(String name, args) {
		if (APPENDERS.containsKey(name) && args) {
			Map constructorArgs = args[0] instanceof Map ? args[0] : [:]
			if (!constructorArgs.encoder) {
				constructorArgs.encoder = createDefaultEncoder()
			}
			Appender appender = APPENDERS[name].newInstance()
			appender.context = context
			BeanUtils.populate appender, constructorArgs

			if (appender instanceof RollingFileAppender) {
				RollingFileAppender rolling = appender
				if (rolling.rollingPolicy) {
					rolling.rollingPolicy.parent = rolling
					if (rolling.rollingPolicy instanceof ContextAware) {
						rolling.rollingPolicy.context = context
					}
					rolling.rollingPolicy.start()
				}
				if (rolling.triggeringPolicy && rolling.triggeringPolicy != rolling.rollingPolicy) {
					if (rolling.triggeringPolicy instanceof ContextAware) {
						rolling.triggeringPolicy.context = context
					}
					rolling.triggeringPolicy.start()
				}
			}
			else if (appender instanceof AsyncAppender) {
				(appender as AsyncAppender).addAppender(appenders[constructorArgs['ref']])
			}

			if (appender.name) {
				appenders[appender.name] = appender
			}
			else {
				LogLog.error "Appender of type $name doesn't define a name attribute, and hence is ignored."
			}

			appender.start()
			return appender
		}

		if (ENCODERS.containsKey(name) && args) {
			Encoder encoder = ENCODERS[name].newInstance(args[0])
			created << encoder
			return encoder
		}

		if (isCustomEnvironmentMethod(name, args)) {
			return invokeCallable(args[0])
		}

		LogLog.error "Method missing when configuring Logback: $name"
	}

	protected boolean isCustomEnvironmentMethod(String name, args) {
		Environment.current == Environment.CUSTOM &&
			Environment.current.name == name &&
			args && (args[0] instanceof Closure)
	}

	void configure() {
		configure {}
	}

	void environments(Closure callable) {
		callable.delegate = new EnvironmentsLogbackConfig(this)
		callable.resolveStrategy = Closure.DELEGATE_FIRST
		callable.call()
	}

	def invokeCallable(Closure callable) {
		callable.delegate = this
		callable.resolveStrategy = Closure.DELEGATE_FIRST
		callable.call()
	}

	/**
	 * Configure Logback from a map whose values are DSL closures. This simply
	 * calls the closures in the order they come out of the map's iterator.
	 * This is to allow configuration like:
	 * <pre>
	 * logback.main = {
	 *     // main Logback configuration in Config.groovy
	 * }
	 *
	 * logback.extra = {
	 *     // additional Logback configuration in an external config file
	 * }
	 * </pre>
	 * In this situation, <code>config.logback</code> is a ConfigObject, which is
	 * an extension of LinkedHashMap, and thus returns its sub-keys in order of
	 * definition.
	 */
	void configure(Map callables) {
		configure(callables.values())
	}

	/**
	 * Configure Logback from a <i>collection</i> of DSL closures by calling the
	 * closures one after another in sequence.
	 */
	void configure(Collection callables) {
		configure { root ->
			for (Closure c in callables) {
				c.delegate = delegate
				c.resolveStrategy = resolveStrategy
				c.call(root)
			}
		}
	}

	void configure(Closure callable) {

		ch.qos.logback.classic.Logger root = getRootLogger()
		root.setLevel Level.ERROR

		Appender consoleAppender = createConsoleAppender()
		appenders.stdout = consoleAppender

		error 'org.springframework', 'org.hibernate'

		callable.delegate = this
		callable.resolveStrategy = Closure.DELEGATE_FIRST

		try {
			callable.call root

			if (!root.iteratorForAppenders().hasNext()) {
				root.addAppender appenders.stdout
			}
			ch.qos.logback.classic.Logger logger = LoggerFactory.getLogger('StackTrace')
			logger.additive = false
			Appender fileAppender = createFullstackTraceAppender()
			if (!logger.iteratorForAppenders().hasNext()) {
				logger.addAppender fileAppender
			}
		}
		catch (Exception e) {
			LogLog.error "WARNING: Exception occured configuring Logback logging: $e.message", e
		}
	}

	protected Appender createConsoleAppender() {
		Appender consoleAppender = Environment.isWarDeployed() ?
			new ConsoleAppender() :
			new LogbackConsoleAppender(config)
		consoleAppender.encoder = createDefaultEncoder()
		consoleAppender.name = 'stdout'
		appenders.console = consoleAppender
		created << consoleAppender
		consoleAppender
	}

	protected Appender createFullstackTraceAppender() {
		if (appenders.stacktrace) {
			return appenders.stacktrace
		}

		FileAppender fileAppender = new FileAppender(
			context: context,
			encoder: createDefaultEncoder(),
			name: 'stacktraceLog')

		File targetDir = BuildSettingsHolder.getSettings()?.getProjectTargetDir()
		targetDir?.mkdirs()
		fileAppender.file = targetDir ? "${targetDir.absolutePath}/stacktrace.log" : 'stacktrace.log'

		fileAppender.start()
		appenders.stacktrace = fileAppender
		fileAppender
	}

	Logger root(Closure c) {
		Logger root = getRootLogger()

		if (c) {
			c.delegate = new RootLogbackConfig(root, this)
			c.resolveStrategy = Closure.DELEGATE_FIRST
			c.call()
		}

		root
	}

	void appenders(Closure callable) {
		callable.delegate = this
		callable.resolveStrategy = Closure.DELEGATE_FIRST
		callable.call()
	}

	Appender appender(Map name, Appender instance) {
		if (!name || !instance) {
			return
		}

		String appenderName = name.values().iterator().next()
		appenders[appenderName] = instance
		instance.name = appenderName
		created << instance
		instance
	}

	Appender appender(Appender instance) {
		if (!instance || !instance.name) {
			LogLog.error "Appender [$instance] is null or does not define a name."
			return
		}

		appenders[instance.name] = instance
		created << instance
		instance
	}

	void off(Map appenderAndPackages) {
		setLogLevelForAppenderToPackageMap(appenderAndPackages, Level.OFF)
	}

	void fatal(Map appenderAndPackages) {
		setLogLevelForAppenderToPackageMap(appenderAndPackages, Level.ERROR)
	}

	void error(Map appenderAndPackages) {
		setLogLevelForAppenderToPackageMap(appenderAndPackages, Level.ERROR)
	}

	void warn(Map appenderAndPackages) {
		setLogLevelForAppenderToPackageMap(appenderAndPackages, Level.WARN)
	}

	void info(Map appenderAndPackages) {
		setLogLevelForAppenderToPackageMap(appenderAndPackages, Level.INFO)
	}

	void debug(Map appenderAndPackages) {
		setLogLevelForAppenderToPackageMap(appenderAndPackages, Level.DEBUG)
	}

	void trace(Map appenderAndPackages) {
		setLogLevelForAppenderToPackageMap(appenderAndPackages, Level.TRACE)
	}

	void all(Map appenderAndPackages) {
		setLogLevelForAppenderToPackageMap(appenderAndPackages, Level.ALL)
	}

	protected setLogLevelForAppenderToPackageMap(Map appenderAndPackages, Level level) {

		boolean additivity = appenderAndPackages.additivity == null ? true : appenderAndPackages.remove('additivity')

		appenderAndPackages.each { appender, packages ->
			eachLogger(packages) { Logger logger ->
				logger.level = level
				if (appenders.containsKey(appender)) {
					logger.addAppender appenders[appender]
					logger.additive = additivity
				}
				else {
					LogLog.error "Appender $appender not found configuring logger ${logger.name}"
				}
			}
		}
	}

	void eachLogger(packages, Closure callable) {
		if (packages instanceof CharSequence) {
			callable LoggerFactory.getLogger(packages.toString())
			return
		}

		for (p in packages) {
			p = p?.toString()
			if (p) {
				callable LoggerFactory.getLogger(p)
			}
		}
	}

	void off(Object[] packages) {
		setLevel packages, Level.OFF
	}

	void fatal(Object[] packages) {
		setLevel packages, Level.ERROR
	}

	void error(Object[] packages) {
		setLevel packages, Level.ERROR
	}

	void warn(Object[] packages) {
		setLevel packages, Level.WARN
	}

	void info(Object[] packages) {
		setLevel packages, Level.INFO
	}

	void debug(Object[] packages) {
		setLevel packages, Level.DEBUG
	}

	void trace(Object[] packages) {
		setLevel packages, Level.TRACE
	}

	void all(Object[] packages) {
		setLevel packages, Level.ALL
	}

	protected void setLevel(Object[] packages, Level level) {
		eachLogger(packages) { logger -> logger.level = level }
	}

	void removeAppender(String name) {
		getRootLogger().detachAppender(name)
	}

	/**
	 * For objects that need to be initialized with the context and started but aren't created
	 * using the DSL.
	 * @param object the object to be initialized
	 * @param now if true, initialize now, otherwise wait until the end with the rest
	 * @return the object
	 */
	def dslInit(object, boolean now = true) {
		if (now) {
			init object
		}
		else {
			created << object
		}
		object
	}

	void init(object) {
		if (object instanceof ContextAware) {
			object.context = context
		}
		if (object instanceof LifeCycle) {
			object.start()
		}
	}

	protected ch.qos.logback.classic.Logger getRootLogger() {
		LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
	}

	protected Encoder createDefaultEncoder() {
		def encoder = new PatternLayoutEncoder(pattern: DEFAULT_ENCODER_PATTERN)
		created << encoder
		encoder
	}
}
