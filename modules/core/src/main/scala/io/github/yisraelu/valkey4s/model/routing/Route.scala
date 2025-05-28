package io.github.yisraelu.valkey4s.model.routing

import io.github.yisraelu.valkey4s.model.routing.Route.SingleNodeRoute.SlotKey.SlotType

sealed trait Route


object Route {

  sealed trait SingleNodeRoute extends Route

  sealed trait MultiNodeRoute extends Route
  object MultiNodeRoute {

  }

  object SingleNodeRoute {
    final case class ByAddress(host:String,port:Short) extends SingleNodeRoute
    final case class SlotKey(slot:String,slotType:SlotType) extends SingleNodeRoute
    final case class SlotId(slotId:Short,slotType: SlotType) extends SingleNodeRoute
    sealed trait Simple extends SingleNodeRoute
    object Simple {
      case object Random extends Simple
    }

    object SlotKey {
      sealed trait SlotType
      object SlotType {
        case object Primary extends SlotType
        case object Replica extends SlotType
      }
    }
  }

}
