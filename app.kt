import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter
import kotlin.math.*
import kotlin.random.Random
import kotlin.system.exitProcess

private const val eps = 0.00000001
private const val u = 0.97
private const val maxVelocity = 1.0
private const val radius = 1.0 / 11.0 / 2.0

private val random = Random.Default

data class Vector(val x: Double, val y: Double) {
    operator fun minus(vector: Vector): Vector {
        return Vector(this.x - vector.x, this.y - vector.y)
    }

    operator fun plus(vector: Vector): Vector {
        return Vector(this.x + vector.x, this.y + vector.y)
    }

    fun getLength(): Double {
        return sqrt(x * x + y * y)
    }

    fun norm(): Vector {
        val length = getLength()
        val result = Vector(x / length, y / length)

        if (result.x.isFinite() && result.y.isFinite()) {
            return result
        }

        return getRandomNormVector()
    }

    operator fun times(value: Double): Vector {
        return Vector(x * value, y * value)
    }
}

data class Body(val point: Vector, val velocity: Vector)

private fun collision(bodies: Pair<Body, Body>): Pair<Body, Body> {
    val firstVelocity = bodies.first.velocity.norm()
    val secondVelocity = bodies.second.velocity.norm()

    val diffPoint = (bodies.second.point - bodies.first.point).norm()

    val cosPhi = diffPoint.x
    val sinPhi = diffPoint.y

    val cosPhiSqr = cosPhi * cosPhi
    val sinPhiSqr = sinPhi * sinPhi
    val cosSinPhi = cosPhi * sinPhi

    val firstLength = bodies.first.velocity.getLength()
    val secondLength = bodies.second.velocity.getLength()

    val newFirstVelocity =
        getVelocity(secondLength, secondVelocity, cosPhiSqr, cosSinPhi, firstLength, firstVelocity, sinPhiSqr)

    val newSecondVelocity =
        getVelocity(firstLength, firstVelocity, cosPhiSqr, cosSinPhi, secondLength, secondVelocity, sinPhiSqr)

    return Pair(
        Body(bodies.first.point, newFirstVelocity),
        Body(bodies.second.point, newSecondVelocity)
    )
}

private fun getVelocity(
    secondLength: Double,
    secondVelocity: Vector,
    cosPhiSqr: Double,
    cosSinPhi: Double,
    firstLength: Double,
    firstVelocity: Vector,
    sinPhiSqr: Double
): Vector {
    return Vector(
        secondLength * (secondVelocity.x * cosPhiSqr + secondVelocity.y * cosSinPhi) -
                firstLength * (firstVelocity.y * cosSinPhi - firstVelocity.x * sinPhiSqr),
        secondLength * (secondVelocity.x * cosSinPhi + secondVelocity.y * sinPhiSqr) +
                firstLength * (firstVelocity.y * cosPhiSqr - firstVelocity.x * cosSinPhi)
    )
}

private fun getRandomNormVector(): Vector {
    val phi = random.nextDouble(2 * Math.PI)
    return Vector(cos(phi), sin(phi))
}

private fun getRandomVector(maxRadius: Double): Vector {
    return getRandomNormVector() * random.nextDouble(maxRadius)
}

private fun generatePoints(number: Int): List<Vector> {
    val result = mutableListOf<Body>()

    val sqrtNumber = sqrt(number.toDouble()).toInt()
    val maxRowColumnCount = if (sqrtNumber * sqrtNumber == number) sqrtNumber else sqrtNumber + 1

    val fullRowsCount = number / maxRowColumnCount
    val lastRowColumnCount = number % maxRowColumnCount

    val widthHeight = 1.0 / 10.0

    val border = 0.0 + (1.0 - (maxRowColumnCount * widthHeight)) / 2

    for (i in 0 until fullRowsCount) {
        for (j in 0 until maxRowColumnCount) {
            result.add(
                Body(
                    Vector(border + (i + 0.5) * widthHeight, border + (j + 0.5) * widthHeight),
                    getRandomVector(maxVelocity)
                )
            )
        }
    }

    if (lastRowColumnCount != 0) {
        val lastRowColumnWidth = (1.0 - 2.0 * border) / lastRowColumnCount

        for (j in 0 until lastRowColumnCount) {
            result.add(
                Body(
                    Vector(border + (fullRowsCount + 0.5) * widthHeight, border + (j + 0.5) * lastRowColumnWidth),
                    getRandomVector(maxVelocity)
                )
            )
        }
    }

    return simulate(result, border)
}

private fun simulate(result: MutableList<Body>, border: Double): List<Vector> {
    while (hasAtLeastOneVelocity(result)) {
        val k1 = analyseInnerCollisions(result)
        val k2 = analyseOuterCollisions(result, border)

        val k = if (k1 < k2) k1 else k2

        if (k.isInfinite()) {
            break
        }

        move(result, k)

        for (i in result.indices) {
            for (j in i + 1 until result.size) {
                if ((2.0 * radius - (result[i].point - result[j].point).getLength()).absoluteValue < eps) {
                    val before = getK(Pair(result[i], result[j]))
                    val collision = collision(Pair(result[i], result[j]))
                    val after = getK(Pair(collision.first, collision.second))
                    if (after < before) {
                        result[i] = collision.first
                        result[j] = collision.second
                    }
                }
            }
        }

        val topLeftBorder = getTopLeftBorder(border)
        val rightBottomBorder = getRightBottomBorder(border)

        for (i in result.indices) {
            if (result[i].velocity.x.sign > 0.0 && (result[i].point.x - rightBottomBorder).absoluteValue < eps ||
                result[i].velocity.x.sign < 0.0 && (result[i].point.x - topLeftBorder).absoluteValue < eps
            ) {
                result[i] = Body(result[i].point, Vector(-result[i].velocity.x, result[i].velocity.y))
            }

            if (result[i].velocity.y.sign > 0.0 && (result[i].point.y - rightBottomBorder).absoluteValue < eps ||
                result[i].velocity.y.sign < 0.0 && (result[i].point.y - topLeftBorder).absoluteValue < eps
            ) {
                result[i] = Body(result[i].point, Vector(result[i].velocity.x, -result[i].velocity.y))
            }
        }
    }

    return result.map { it.point }
}

private fun move(bodies: MutableList<Body>, k: Double) {
    for (i in bodies.indices) {
        bodies[i] = Body(bodies[i].point + bodies[i].velocity * k, bodies[i].velocity * u.pow(k))
    }
}

private fun analyseOuterCollisions(bodies: List<Body>, border: Double): Double {
    var k = Double.POSITIVE_INFINITY

    for (i in bodies.indices) {
        val body = bodies[i]

        val topLeftBorder = getTopLeftBorder(border)
        val rightBottomBorder = getRightBottomBorder(border)

        val newK = listOf(
            getK(body.point.x, body.velocity.x, rightBottomBorder),
            getK(body.point.x, body.velocity.x, topLeftBorder),
            getK(body.point.y, body.velocity.y, rightBottomBorder),
            getK(body.point.y, body.velocity.y, topLeftBorder)
        ).min() ?: Double.POSITIVE_INFINITY

        if (newK < k) {
            k = newK
        }
    }

    return k
}

private fun getRightBottomBorder(border: Double) = 1.0 - border - radius

private fun getTopLeftBorder(border: Double) = 0.0 + border + radius

private fun getK(point: Double, velocity: Double, border: Double): Double {
    val k = (border - point) / velocity

    if (k.sign > 0.0) {
        return k
    }

    return Double.POSITIVE_INFINITY
}

private fun analyseInnerCollisions(bodies: List<Body>): Double {
    var k: Double = Double.POSITIVE_INFINITY

    for (i in bodies.indices) {
        for (j in i + 1 until bodies.size) {
            val newK = getK(Pair(bodies[i], bodies[j]))

            if (newK.isFinite() && newK.sign > 0.0 && newK < k) {
                k = newK
            }
        }
    }

    return k
}

private fun hasAtLeastOneVelocity(bodies: List<Body>): Boolean {
    return bodies.any { it.velocity.getLength() > eps }
}

private fun getK(bodies: Pair<Body, Body>): Double {
    val p = bodies.first.point.x - bodies.second.point.x
    val q = bodies.first.point.y - bodies.second.point.y

    val a = bodies.first.velocity.x - bodies.second.velocity.x
    val b = bodies.first.velocity.y - bodies.second.velocity.y

    val o = a * q - b * p
    val r = a * a + b * b
    val sqrtD = sqrt(4.0 * radius * radius * r - o * o)

    if (sqrtD.isNaN()) {
        return Double.POSITIVE_INFINITY
    }

    val s = a * p + b * q

    return (-s - sqrtD) / r

}

private fun checkRadius(points: List<Vector>): Boolean {
    for (i in points.indices) {
        for (j in i + 1 until points.size) {
            if ((points[i] - points[j]).getLength() < 2.0 * radius) {
                return false
            }
        }

        val edge = 0.0 + radius..1.0 - radius
        if (points[i].x !in edge || points[i].y !in edge) {
            return false
        }
    }

    return true
}

private fun writeRect(writer: XMLStreamWriter, xy: String, wh: String) {
    writer.writeStartElement("rect")
    writer.writeAttribute("x", xy)
    writer.writeAttribute("y", xy)
    writer.writeAttribute("width", wh)
    writer.writeAttribute("height", wh)
    writer.writeAttribute("style", "stroke: #000000; fill:none;")
    writer.writeEndElement()
}

private fun writePointCoord(writer: XMLStreamWriter, attrName: String, attrValue: Double) {
    writer.writeAttribute(attrName, String.format("%fin", attrValue * 11.0 + 0.5))
}

private fun writerProlog(writer: XMLStreamWriter) {
    writer.writeStartDocument()
    writer.writeDTD("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">")
    writer.writeStartElement("svg")
    writer.writeAttribute("version", "1.1")
    writer.writeAttribute("width", "12in")
    writer.writeAttribute("height", "12in")
}

private fun writeEpilog(writer: XMLStreamWriter) {
    writer.writeEndDocument()
}

fun writeText(writer: XMLStreamWriter, number: Int, rotate: Int) {
    writer.writeStartElement("text")
    writer.writeAttribute("class", "num")
    writer.writeAttribute("x", "-3in")
    writer.writeAttribute("y", "-3in")
    writer.writeAttribute("dominant-baseline", "middle")
    writer.writeAttribute("text-anchor", "middle")
    writer.writeAttribute("transform", String.format("rotate(%d)", rotate))
    writer.writeCharacters(number.toString())
    writer.writeEndElement()
}

private fun <R> save(output: String, block: (XMLStreamWriter) -> R) {
    Files.newOutputStream(Paths.get(output)).buffered().writer().use {
        val writer = XMLOutputFactory.newFactory().createXMLStreamWriter(it)
        writerProlog(writer)

        block(writer)

        writeEpilog(writer)
    }
}

private fun save(points: List<Vector>, output: String) {
    save(output) {
        writeRect(it, "0in", "12in")

        for (point in points) {
            it.writeStartElement("circle")
            writePointCoord(it, "cx", point.x)
            writePointCoord(it, "cy", point.y)
            it.writeAttribute("r", "0.375in")
            it.writeAttribute("style", "stroke: #FF0000; fill:#FF0000;")
            it.writeEndElement()
        }
    }
}

private fun save(number: Int, output: String) {
    save(output) {
        it.writeStartElement("style")
        it.writeCharacters(".num { font: bold 1in sans-serif; } ")
        it.writeEndElement()

        it.writeStartElement("svg")
        it.writeAttribute("x", "6in")
        it.writeAttribute("y", "6in")

        writeText(it, number, 0)
        writeText(it, number, 90)
        writeText(it, number, 180)
        writeText(it, number, 270)

        it.writeEndElement()
    }
}

private fun getNumberOrThrow(value: String): Int {
    val number = value.toIntOrNull()

    if (number == null || number !in 1..100) {
        throw IllegalStateException(String.format("Expected number between 1 and 100, but got '%s'.", value))
    }

    return number
}

fun main(args: Array<String>) {
    if (args.size < 4) {
        println("\tUsage: <app> <number> front|back <output>")
        exitProcess(1)
    }

    val number = getNumberOrThrow(args[1])
    val isFront = "front" == args[2]
    val output = args[3]

    if (isFront) {
        var points: List<Vector>
        do {
            points = generatePoints(number)
        } while (!checkRadius(points))
        save(points, output)
    } else {
        save(number, output)
    }
}
