import grails.plugin.logback.LogbackConfig
import grails.plugin.logback.LogbackConfigListener
import grails.util.Metadata

import java.util.logging.LogManager

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.slf4j.bridge.SLF4JBridgeHandler

class LogbackGrailsPlugin {

	String version = '0.4.0'
	String grailsVersion = '2.0 > *'
	String title = 'Logback Plugin'
	String author = 'Burt Beckwith'
	String authorEmail = 'burt@burtbeckwith.com'
	String description = 'Replaces Log4j with Logback for logging'
	String documentation = 'http://grails.org/plugin/logback'
	String packaging = 'binary'
	def loadBefore = ['core', 'grails-logging']
	def evict = ['grails-logging', 'grails-plugin-log4j']

	String license = 'APACHE'
	def organization = [name: 'Grails', url: 'http://grails.org/']
	def issueManagement = [url: 'https://github.com/grails-plugins/grails-logback/issues']
	def scm = [url: 'https://github.com/grails-plugins/grails-logback']

	def doWithWebDescriptor = { xml ->

		initLogging application

		def mappingElement = xml.'filter-mapping'
		mappingElement = mappingElement[mappingElement.size() - 1]

		mappingElement + {
			listener {
				'listener-class'(LogbackConfigListener.name)
			}
		}
	}

	def doWithSpring = {
		if (application.config?.grails?.logging?.jul?.usebridge) {
			LogManager.logManager.readConfiguration new ByteArrayInputStream(".level=INFO".bytes)
			SLF4JBridgeHandler.install()
		}

		if (application.warDeployed) {
			// only initialize here if deployed as a war since doWithWebDescriptor isn't called
			initLogging application
		}
	}

	def onConfigChange = { event ->
		LogbackConfig.initialize event.source
	}

	private void initLogging(GrailsApplication application) {
		if (!Metadata.current.getApplicationName()) {
			// don't configure in the plugin
			return
		}

		LogbackConfig.initialize application.config
	}
}
