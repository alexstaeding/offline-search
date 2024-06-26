package io.github.alexstaeding.offlinesearch.network

import io.github.alexstaeding.offlinesearch.meta.PartialKey

trait HashingAlgorithm[V] {
  def hash(value: PartialKey[V]): NodeId
}
