package fr.xebia.dataviz

import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.file.impl.PathAdjuster
import org.vertx.java.core.impl.VertxInternal

/**
 * User: mounirboudraa
 * Date: 11/12/2013
 * Time: 11:29
 */
class BrowseFileVerticle extends Verticle {

    def start() {


        def mapInsee = [:]


        def start = System.currentTimeMillis()

        findOnDisk("insee.csv").splitEachLine(";") { fields ->


            mapInsee.put(fields[3].padLeft(5, "0"), [
                    code: fields[3],
                    postcode: fields[1],
                    formattedName: fields[0],
                    region: fields[2]
            ])
        }


        new File("out.txt").withWriter {
            out ->
                findOnDisk("HIST_POP_COM_RP10.csv").splitEachLine(";") { fields ->
                    def insee = mapInsee.get(fields[0])

                    if (fields[0].length() > 0 && !fields[0].startsWith("2A") && !fields[0].startsWith("2B") &&new Integer(fields[0]) >= 27000) {

                        Map message = [
                                insee: insee,
                                prettyName: fields[1],
                                population: fields[2]
                        ]

                        vertx.eventBus.send("fr.xebia.dataviz.gatherInfo", message) { response ->
                            println response
                        }

                        mapInsee.remove(insee)
                    }


                }
        }


        def end = System.currentTimeMillis()

        println " SUCCESSFUL =>  ${(end - start) / 6000 }  minutes "

    }



    private File findOnDisk(String resourceRelativePath) {
        VertxInternal core = vertx.toJavaVertx() as VertxInternal
        String pathToDisk = PathAdjuster.adjust(core, resourceRelativePath)
        new File(pathToDisk)
    }


}

