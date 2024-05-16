package io.github.alexstaeding.offlinesearch.network

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import org.apache.logging.log4j.Logger

import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.net.{InetSocketAddress, URI}
import java.util.concurrent.Executors
import scala.concurrent.*
import scala.jdk.FutureConverters.CompletionStageOps

class HttpNetworkAdapter[V: JsonValueCodec](
    private val bindAddress: InetSocketAddress,
    private val onReceive: EventReceiver[V],
)(using logger: Logger)
    extends NetworkAdapter[V] {

  private val client = HttpClient.newHttpClient()
  private val server = HttpServer.create(bindAddress, 10)
  implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

  {
    server.createContext(
      "/api/v1/message",
      (exchange: HttpExchange) => {
        logger.info("Received message")
        val eventAnswer: Either[RedirectEvent[V], AnswerEvent[V, _]] =
          readFromStream(exchange.getRequestBody)(using RequestEvent.codec) match
            case pingEvent: PingEvent[V]             => onReceive.receivePing(pingEvent)
            case findNodeEvent: FindNodeEvent[V]     => onReceive.receiveFindNode(findNodeEvent)
            case findValueEvent: FindValueEvent[V]   => onReceive.receiveFindValue(findValueEvent)
            case storeValueEvent: StoreValueEvent[V] => onReceive.receiveStoreValue(storeValueEvent)

        val response = eventAnswer match
          case Left(redirect) => writeToString(redirect)(using AnswerEvent.codec)
          case Right(answer)  => writeToString(answer)(using AnswerEvent.codec)

        exchange.sendResponseHeaders(200, response.length)
        exchange.getResponseBody.write(response.getBytes)
        exchange.close()
      },
    )
    server.setExecutor(ec)
    server.start()
    logger.info("Started server on " + bindAddress)
  }

  override def send[C, A <: AnswerEvent[V, C], R <: RequestEvent[V, C] { type Answer <: A }](
      nextHop: InetSocketAddress,
      event: R,
  ): Future[Either[RedirectEvent[V], A]] = {
    val request = HttpRequest
      .newBuilder()
      .uri(URI.create(s"http://${nextHop.getAddress.getHostAddress}:${nextHop.getPort}/api/v1/message"))
      .POST(BodyPublishers.ofString(writeToString(event)(using RequestEvent.codec)))
      .build()

    client
      .sendAsync(request, BodyHandlers.ofString())
      .thenApply { response => readFromString(response.body())(using AnswerEvent.codec) }
      .thenApply {
        case redirect: RedirectEvent[V] => Left(redirect)
        case answer: A                  => Right(answer)
      }
      .asScala
  }
}

object HttpNetworkAdapter extends NetworkAdapter.Factory {
  def create[V: JsonValueCodec, C, R <: RequestEvent[V, C]](
      bindAddress: InetSocketAddress,
      onReceive: EventReceiver[V],
  )(using logger: Logger): NetworkAdapter[V] = HttpNetworkAdapter(bindAddress, onReceive)
}