import cats.effect.*
import com.scalavr.protos.parkings.*
import io.grpc.*
import fs2.Stream
import fs2.grpc.syntax.all.*
import io.grpc.netty.shaded.io.grpc.netty.{NettyChannelBuilder, NettyServerBuilder}

class ParkingService extends ParkingServiceFs2Grpc[IO, Metadata] {
  override def sendReservation(request: fs2.Stream[IO, ReservationRequest], ctx: Metadata): fs2.Stream[IO, ReservationReply] =
    request.map { replyReq =>
      ReservationReply(
        replyReq.code,
        replyReq.park,
        if(replyReq.park.map(_.amount).sum>0) true else false
      )
    }
}

object ParkServer extends IOApp.Simple {
  val gRPCserver: Resource[IO, Server] =
    for {
      service <- ParkingServiceFs2Grpc.bindServiceResource[IO](new ParkingService)
      server <- Resource.make {
        IO {
          NettyServerBuilder
            .forPort(9998)
            .addService(service)
            .build()
            .start()
        }
      }(s => IO(s.shutdownNow()))
    } yield server

  override def run: IO[Unit] =
    gRPCserver.use { _ =>
      IO.println("Reservation microservice started") *> IO.never
    }
}


object ParkClient {
  val resource = NettyChannelBuilder
    .forAddress("localhost", 9998)
    .usePlaintext()
    .resource[IO]
    .flatMap(channel => ParkingServiceFs2Grpc.stubResource(channel))
}

object gRPCdemo extends IOApp.Simple {
  override def run: IO[Unit] = {
    ParkClient.resource.use { parkingService =>
      val requestStream = Stream.eval(IO(
        ReservationRequest(
          0,
          List(Park("SUV", 5, 13))
        )
      ))

      parkingService
        .sendReservation(requestStream, new Metadata())
        .compile
        .toList
        .flatMap(replies => IO.println(replies))
    }
  }
}
