package fr.xebia.dataviz

import com.englishtown.vertx.elasticsearch.ElasticSearch
import fr.xebia.dataviz.es.ElasticSearchClientVerticle
import org.vertx.groovy.platform.Verticle

/**
 * User: mounirboudraa
 * Date: 11/12/2013
 * Time: 11:27
 */
class MainVerticle extends Verticle {

    def start() {
        container.deployWorkerVerticle('groovy:' + ElasticSearchClientVerticle.class.name, container.config)
        container.deployWorkerVerticle('groovy:' + BrowseFileVerticle.class.name, container.config, 1)
        container.deployWorkerVerticle('groovy:' + GatherInfoVerticle.class.name, container.config, 1)

    }


}
