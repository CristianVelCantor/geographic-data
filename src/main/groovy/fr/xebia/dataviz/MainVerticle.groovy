package fr.xebia.dataviz

import com.englishtown.vertx.elasticsearch.ElasticSearch
import org.vertx.groovy.platform.Verticle

/**
 * User: mounirboudraa
 * Date: 11/12/2013
 * Time: 11:27
 */
class MainVerticle extends Verticle {

    def start() {



        container.deployWorkerVerticle('groovy:' + ElasticSearch.class.name, container.config) { response ->

            println response
            if (response.succeeded()) {
                println "SUCCESS.............."
                        container.deployWorkerVerticle('groovy:' + BrowseFileVerticle.class.name, container.config, 1)

//                    startedResult.setResult(null);
            } else {
                println "FAILURE..............${response.cause()}"
                response.cause().printStackTrace()
//                    startedResult.setFailure(result.cause());
            }

        }

    }


}
