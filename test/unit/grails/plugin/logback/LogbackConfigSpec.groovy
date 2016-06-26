package grails.plugin.logback

import grails.plugin.dumbster.Dumbster
import groovy.sql.Sql

import java.sql.Connection
import java.sql.DriverManager

import org.slf4j.LoggerFactory

import spock.lang.Specification
import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.db.DBAppender
import ch.qos.logback.classic.net.SMTPAppender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.rolling.RollingFileAppender

import com.dumbster.smtp.SmtpMessage

class LogbackConfigSpec extends Specification {

	private Dumbster dumbster = new Dumbster()

	void setup() {
		dumbster.grailsApplication = [
			config: [dumbster: [enabled: true]],
			mainContext: [containsBean: { String -> false }]]
	}

	void cleanup() {
		LoggerFactory.ILoggerFactory.reset()
		LoggerContext loggerFactory = LoggerFactory.ILoggerFactory
		loggerFactory.loggerCache.clear()
		dumbster.reset()
	}

	void testParseSimple() {
		when:
		String config = """
logback = {
	error 'org.codehaus.groovy.grails',
	      'org.springframework',
	      'org.hibernate',
	      'net.sf.ehcache.hibernate'
	debug 'com.foo.bar'
}
"""

		parse config

		def names = ['ROOT', 'StackTrace'] + split('org.codehaus.groovy.grails') + split('org.springframework') +
			split('org.hibernate') + split('net.sf.ehcache.hibernate') + split('com.foo.bar') +
			['grails.plugin.dumbster', 'grails.plugin.dumbster.Dumbster']

		def loggerNames = findLoggerNames()

		then:
		loggerNames.containsAll names.unique()

		when:
		def logger = LoggerFactory.getLogger('org.codehaus.groovy.grails.Foo')

		then:
		logger
		null == logger.level
		Level.ERROR == logger.effectiveLevel
	}

	void testFileAppender() {
		when:
		String config = '''
logback = {
	appenders {
		file name: 'mylog', file:'/tmp/mylog.log'
	}

	info mylog: 'grails.app.controllers.BookController'

	root {
		debug 'stdout', 'mylog'
	}
}'''

		parse config

		def appenders = root.iteratorForAppenders().collect { it }.sort { it.getClass().name }

		then:
		2 == appenders.size()
		appenders[0] instanceof FileAppender
		appenders[1] instanceof LogbackConsoleAppender
		Level.DEBUG == root.level

		appenders[0].append
		'/tmp/mylog.log' == appenders[0].fileName
		'mylog' == appenders[0].name

		when:
		def logger = LoggerFactory.getLogger('grails.app.controllers.BookController')
		appenders = logger.iteratorForAppenders().collect { it }.sort { it.getClass().name }

		then:
		1 == appenders.size()
		appenders[0] instanceof FileAppender
		Level.INFO == logger.level
	}

	void testAsyncAppender() {
		when:
		String config = '''
	logback = {
		appenders {
		file name: "mylog", file: "/tmp/mylog.log"
		async name: "myasync", ref: "mylog"
	}

	info myasync: "grails.app.controllers.BookController"

	root {
		debug "stdout", "myasync"
	}
}'''

		parse config

		def appenders = root.iteratorForAppenders().collect { it }.sort { it.getClass().name }

		then:
		2 == appenders.size()
		appenders[0] instanceof AsyncAppender
		appenders[1] instanceof LogbackConsoleAppender
		Level.DEBUG == root.level

		when:
		AsyncAppender async = appenders[0] as AsyncAppender

		then:
		'/tmp/mylog.log' == (async.getAppender("mylog") as FileAppender).file
		'myasync' == appenders[0].name

		when:
		def logger = LoggerFactory.getLogger('grails.app.controllers.BookController')
		appenders = logger.iteratorForAppenders().collect { it }.sort { it.getClass().name }

		then:
		1 == appenders.size()
		appenders[0] instanceof AsyncAppender
		Level.INFO == logger.level
	}

	void testRollingFileAppender() {
		when:
		String config = '''
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy

logback = {
	appenders {
		rollingFile name: "myAppender", file: "/tmp/myApp.log",
		            encoder: pattern(pattern: "%-4relative [%thread] %-5level %logger{35} - %msg%n"),
		            rollingPolicy: new TimeBasedRollingPolicy(fileNamePattern: "mylog-%d{yyyy-MM-dd}.txt", maxHistory: 30)
	}

	trace myAppender: "grails.app.controllers.BookController"
}
'''
		parse config

		def logger = LoggerFactory.getLogger('grails.app.controllers.BookController')

		then:
		Level.TRACE == logger.level

		when:
		def appenders = logger.iteratorForAppenders().collect { it }.sort { it.getClass().name }

		then:
		1 == appenders.size()

		appenders[0] instanceof RollingFileAppender
		appenders[0].append
		'/tmp/myApp.log' == appenders[0].fileName
		'myAppender' == appenders[0].name
		'%-4relative [%thread] %-5level %logger{35} - %msg%n' == appenders[0].encoder.pattern
		appenders[0].triggeringPolicy == appenders[0].rollingPolicy
		30 == appenders[0].rollingPolicy.maxHistory
		'mylog-%d{yyyy-MM-dd}.txt' == appenders[0].rollingPolicy.fileNamePatternStr
	}

	void testFileRolling() {
		when:
		File tempDir = new File(System.getProperty('java.io.tmpdir'))

		tempDir.eachFileRecurse { File file ->
			if (file.file && file.name.startsWith('logback-test')) {
				assert file.delete()
			}
		}

		String config = """
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy

logback = {
	appenders {
		rollingFile name: 'myAppender', file: '${tempDir.path}${File.separatorChar}logback-test.log',
		            encoder: pattern(pattern: '%-4relative [%thread] %-5level %logger{35} - %msg%n'),
		            triggeringPolicy: new SizeBasedTriggeringPolicy(maxFileSize: 100),
		            rollingPolicy: new FixedWindowRollingPolicy(fileNamePattern: '${tempDir.path}${File.separatorChar}logback-test.%i.log')
	}

	info myAppender: 'grails.app.controllers.BookController'
}
"""
		parse config

		Logger logger = LoggerFactory.getLogger('grails.app.controllers.BookController')

		then:
		Level.INFO == logger.level

		when:
		def appenders = logger.iteratorForAppenders().collect { it }.sort { it.getClass().name }

		then:
		1 == appenders.size()

		appenders[0] instanceof RollingFileAppender
		'/tmp/logback-test.log' == appenders[0].fileName
		'100' == appenders[0].triggeringPolicy.maxFileSize
		'/tmp/logback-test.%i.log' == appenders[0].rollingPolicy.fileNamePatternStr

		when:
		root.detachAppender 'stdout'
		10000.times { logger.debug 'this is a debug message' }
		100.times { logger.error '1234567890' }

		int count = 0
		tempDir.eachFileRecurse { File file ->
			if (file.file && file.name.startsWith('logback-test')) {
				count++
			}
		}

		then:
		4 == count
	}

	void testXmlEncoder() {
		when:
		String config = '''
import grails.plugin.logback.MemoryAppender

logback = {
	appenders {
		appender new MemoryAppender(name: "memory", encoder: xml)
	}

	info memory: "grails.app.controllers.BookController"
}'''

		parse config

		Logger logger = LoggerFactory.getLogger('grails.app.controllers.BookController')

		then:
		Level.INFO == logger.level

		when:
		def appenders = logger.iteratorForAppenders().collect { it }.sort { it.getClass().name }

		then:
		1 == appenders.size()

		appenders[0] instanceof MemoryAppender

		when:
		root.detachAppender 'stdout'
		10000.times { logger.debug 'this is a debug message' }

		10.times { logger.error "event$it" }

		String xml = '<events>\n' + appenders[0].renderedOutput.replaceAll('log4j:', '') + '\n</events>'

		def events = parseXml(xml).event

		then:
		10 == events.size()

		events.eachWithIndex { event, int index ->
			assert 'grails.app.controllers.BookController' == event.@logger.text()
			assert event.@timestamp.text() as Long
			assert 'ERROR' == event.@level.text()
			assert 'main' == event.@thread.text()
			assert "event$index" == event.message[0].text()
		}
	}

	void testHtmlEncoder() {
		when:
		String config = '''
import grails.plugin.logback.MemoryAppender

logback = {
	appenders {
		appender new MemoryAppender(name: 'memory', encoder: html(pattern: '%m%n'))
	}

	info memory: 'grails.app.controllers.BookController'
}'''

		parse config

		Logger logger = LoggerFactory.getLogger('grails.app.controllers.BookController')

		then:
		Level.INFO == logger.level

		when:
		def appenders = logger.iteratorForAppenders().collect { it }.sort { it.getClass().name }

		then:
		1 == appenders.size()

		appenders[0] instanceof MemoryAppender

		when:
		root.detachAppender 'stdout'
		10000.times { logger.debug 'this is a debug message' }

		10.times { logger.error "event$it" }

		String xml = appenders[0].renderedOutput + '\n</table></body></html>'
		xml = xml[xml.indexOf('<html>')..-1] // remove the doctype
		def trs = parseXml(xml).body.table.tr

		then:
		11 == trs.size()

		trs.eachWithIndex { tr, int index ->
			if (index == 0) {
				assert 'header' == tr.@class.text()
				return
			}

			int rowNumber = index - 1
			if (rowNumber % 2 == 0) {
				assert 'error even' == tr.@class.text()
			}
			else {
				assert 'error odd' == tr.@class.text()
			}

			assert 2 == tr.td.size()

			assert 'Message' == tr.td[0].@class.text()
			assert "event$rowNumber" == tr.td[0].text()

			assert 'LineSeparator' == tr.td[1].@class.text()
			assert "" == tr.td[1].text().trim()
		}
	}

	void testEchoEncoder() {
		when:
		String config = '''
import grails.plugin.logback.MemoryAppender

logback = {
	appenders {
		appender new MemoryAppender(name: 'memory', encoder: simple)
	}

	info memory: 'grails.app.controllers.BookController'
}'''

		parse config

		Logger logger = LoggerFactory.getLogger('grails.app.controllers.BookController')

		then:
		Level.INFO == logger.level

		when:
		def appenders = logger.iteratorForAppenders().collect { it }.sort { it.getClass().name }

		then:
		1 == appenders.size()

		appenders[0] instanceof MemoryAppender

		when:
		root.detachAppender 'stdout'
		10000.times { logger.debug 'this is a debug message' }

		10.times { logger.error "event$it" }

		def lines = appenders[0].renderedOutput.readLines()

		then:
		10 == lines.size()
		lines.eachWithIndex { String line, int index ->
			assert "[ERROR] event$index" == line.trim()
		}
	}

	void testDbAppender() {
		when:
		String url = 'jdbc:h2:mem:logback'
		String user = 'sa'
		String password = ''
		String driver = 'org.h2.Driver'

		String config = """
import ch.qos.logback.classic.db.DBAppender
import ch.qos.logback.core.db.DriverManagerConnectionSource

logback = {
	appenders {

		def connectionSource = dslInit(new DriverManagerConnectionSource(
				url: "$url", user: "$user", password: "$password", driverClass: "$driver"))

		appender new DBAppender(name: 'db', connectionSource: connectionSource)
	}

	info db: 'grails.app.controllers.BookController'
}"""

		parse config

		String ddl = '''
CREATE TABLE logging_event (
	timestmp BIGINT NOT NULL,
	formatted_message LONGVARCHAR NOT NULL,
	logger_name VARCHAR(256) NOT NULL,
	level_string VARCHAR(256) NOT NULL,
	thread_name VARCHAR(256),
	reference_flag SMALLINT,
	arg0 VARCHAR(256),
	arg1 VARCHAR(256),
	arg2 VARCHAR(256),
	arg3 VARCHAR(256),
	caller_filename VARCHAR(256),
	caller_class VARCHAR(256),
	caller_method VARCHAR(256),
	caller_line CHAR(4),
	event_id IDENTITY NOT NULL);

CREATE TABLE logging_event_property (
	event_id BIGINT NOT NULL,
	mapped_key VARCHAR(254) NOT NULL,
	mapped_value LONGVARCHAR,
	PRIMARY KEY(event_id, mapped_key),
	FOREIGN KEY (event_id) REFERENCES logging_event(event_id));

CREATE TABLE logging_event_exception (
	event_id BIGINT NOT NULL,
	i SMALLINT NOT NULL,
	trace_line VARCHAR(256) NOT NULL,
	PRIMARY KEY(event_id, i),
	FOREIGN KEY (event_id) REFERENCES logging_event(event_id));
'''

		Connection connection = DriverManager.getConnection(url, user, password)
		def sql = Sql.newInstance(url, user, password, driver)
		ddl.split(';').each { sql.executeUpdate it }

		Logger logger = LoggerFactory.getLogger('grails.app.controllers.BookController')

		then:
		Level.INFO == logger.level

		when:
		def appenders = logger.iteratorForAppenders().collect { it }.sort { it.getClass().name }

		then:
		1 == appenders.size()

		appenders[0] instanceof DBAppender

		when:
		root.detachAppender 'stdout'
		10000.times { logger.debug 'this is a debug message' }

		10.times { logger.error "event$it" }
		logger.error "with exception", new Exception('oh no')

		then:
		11 == sql.firstRow('select count(*) c from logging_event').c
		11 == sql.firstRow('select count(*) c from logging_event_property').c
		sql.firstRow('select count(*) c from logging_event_exception').c > 0 // stack trace lines, number could change
	}

	void testSmtpAppender() {
		when:
		dumbster.start()

		String from = 'test@testing.com'
		String host = 'localhost'
		String to = 'to@testing.com'

		String config = """
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.net.SMTPAppender

logback = {
	appenders {

		def layout = dslInit(new PatternLayout(outputPatternAsHeader: false, pattern: '%-4relative %-5level %class - %msg%n'))
		def smtp = new SMTPAppender(name: 'smtp', asynchronousSending: false, from: '$from', smtpHost: '$host',
		                            smtpPort: $dumbster.port, layout: layout)
		smtp.context = context // need this now for the parser that's used for the To address
		smtp.addTo '$to'
		appender smtp
	}

	info smtp: 'grails.app.controllers.BookController'
}"""

		parse config

		Logger logger = LoggerFactory.getLogger('grails.app.controllers.BookController')

		then:
		Level.INFO == logger.level

		when:
		def appenders = logger.iteratorForAppenders().collect { it }.sort { it.getClass().name }

		then:
		1 == appenders.size()

		appenders[0] instanceof SMTPAppender

		when:
		root.detachAppender 'stdout'
		10000.times { logger.debug 'this is a debug message' }

		10.times { logger.error "event$it" }

		then:
		10 == dumbster.messageCount

		dumbster.messages.eachWithIndex { SmtpMessage email, int index ->
			assert "g.a.c.BookController - event$index" == email.getHeaderValue('Subject')
			assert email.body.contains('ERROR')
			assert email.body.contains("event$index")
		}
	}

	private List<String> split(String name) {
		def names = []
		def parts = name.split('\\.')
		parts.eachWithIndex { String part, int i ->
			def sequence = []
			(i + 1).times { int j -> sequence << parts[j] }
			names << sequence.join('.')
		}
		names
	}

	private List<String> findLoggerNames() {
		LoggerContext loggerFactory = LoggerFactory.ILoggerFactory
		List<Logger> loggers = loggerFactory.loggerList

		def loggerNames = loggers.collect { it.name }.sort()
		loggerNames.removeAll split(getClass().name)
		loggerNames.removeAll split(LogbackConfig.name)

		loggerNames
	}

	private Logger getRoot() {
		LoggerFactory.getLogger Logger.ROOT_LOGGER_NAME
	}

	private void parse(String config) {
		LogbackConfig.initialize new ConfigSlurper().parse(config)
	}

	private parseXml(String xml) {
		def slurper = new XmlSlurper()
		slurper.setFeature 'http://apache.org/xml/features/disallow-doctype-decl', true
		slurper.parseText xml
	}
}
