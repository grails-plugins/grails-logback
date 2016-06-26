grails.project.work.dir = 'target'

grails.project.dependency.resolver = 'maven'
grails.project.dependency.resolution = {

	inherits 'global', {
		excludes 'grails-plugin-log4j'
	}

	log 'warn'
	checksums true

	repositories {
		grailsCentral()
		mavenLocal()
		mavenCentral()
	}

	dependencies {
		String logbackVersion = '1.1.7'

		compile "ch.qos.logback:logback-classic:$logbackVersion"
		compile "ch.qos.logback:logback-core:$logbackVersion"
		compile 'commons-beanutils:commons-beanutils-core:1.8.3'
		compile 'org.slf4j:jul-to-slf4j:1.7.21'
		compile 'org.slf4j:slf4j-api:1.7.21'

		test 'org.grails:grails-datastore-test-support:1.0.2-grails-2.4'
	}

	plugins {
		build ':release:3.1.2', ':rest-client-builder:2.1.1', {
			export = false
		}
		test ':dumbster:0.2', {
			export = false
		}
	}
}
