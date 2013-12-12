package fr.xebia.dataviz

import org.vertx.groovy.platform.Verticle

/**
 * User: mounirboudraa
 * Date: 11/12/2013
 * Time: 11:27
 */
class MainVerticle extends Verticle{

    def start() {
        container.deployWorkerVerticle('groovy:' + BrowseFileVerticle.class.name, container.config, 1)
    }


}
