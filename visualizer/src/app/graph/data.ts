import {SimulationLinkDatum, SimulationNodeDatum} from "d3-force";
import {NodeInfo} from "../api/NodeInfoUpdate";

export interface Node extends SimulationNodeDatum, NodeInfo {
  contentUrl?: string
  contentKeys?: string[]
}

export interface Link extends SimulationLinkDatum<Node> {
}

export interface Data {
  nodes: Node[];
  links: Link[];
}
