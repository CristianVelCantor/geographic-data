package fr.xebia.dataviz.es

import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.core.http.HttpClient
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.json.impl.Json
import org.vertx.groovy.core.buffer.Buffer

class ElasticSearchClientVerticle extends Verticle {

    HttpClient esClient

    Map conf = [:]

    def start() {

        conf.host = container.config.host ?: 'localhost'
        conf.port = container.config.port ?: 9200

        esClient = vertx.createHttpClient(port: conf.port, host: conf.host)

        [
                'fr.xebia.dataviz.es.getObject': this.&getObject,
                'fr.xebia.dataviz.es.createObject': this.&createObject,
                'fr.xebia.dataviz.es.query': this.&doQuery
        ].each { eventBusAddress, handler ->
            vertx.eventBus.registerHandler(eventBusAddress, handler)
        }
    }

    void doQuery(Message message) {
        def index = message.body.index
        def entity = message.body.entity
        def query = message.body.query

        esClient.getNow("/$index/$entity/_search?size=20&q=$query") { esResp ->
            def body = new Buffer()
            esResp.dataHandler { buffer -> body << buffer }
            esResp.endHandler {
                message.reply([
                        status: esResp.statusCode,
                        body: Json.decodeValue(body.toString(), Map.class)
                ])
            }
        }
    }

    void createObject(Message message) {
        //println "received creation demand"

        def index = message.body.index
        def entity = message.body.entity
        def id = message.body.id
        def content = message.body.content

        def put = esClient.put("/$index/$entity/$id") { esResp ->
        //def put = esClient.post("/$index/$entity/") { esResp ->
            if(esResp.statusCode!=200 && esResp.statusCode!=201 ){
                println "Error indexing in es : ${esResp.statusCode} for ${id} : ${content.formattedName}"
            }
            def body = new Buffer()
            esResp.dataHandler { buffer -> body << buffer }
            esResp.endHandler {
                message.reply([
                        status: esResp.statusCode,
                        body: Json.decodeValue(body.toString(), Map.class)
                ])
            }
        }

//        put.putHeader("Content-Encoding", "gzip")
        put.putHeader("Accept-Encoding", "compress, gzip")
        put.chunked = true
        put << Json.encode(content)
        put.end()
    }

    void getObject(Message message) {

        def index = message.body.index
        def entity = message.body.entity
        def id = message.body.id

        println "getting from ES"
        esClient.getNow("/$index/$entity/$id") { esResp ->
            def body = new Buffer()
            esResp.dataHandler { buffer -> body << buffer }
            esResp.endHandler {
                println "response ES ended"
                message.reply([
                        status: esResp.statusCode,
                        body: Json.decodeValue(body.toString(), Map.class)
                ])
            }
        }
    }
}
