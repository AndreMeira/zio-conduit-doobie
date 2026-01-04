package conduit.infrastructure.inmemory

import conduit.infrastructure.inmemory.monitor.InMemoryMonitor
import conduit.infrastructure.inmemory.repository.InMemoryRepositoryModule

object InMemoryModule {
  val layer = InMemoryMonitor.layer >>> InMemoryRepositoryModule.layer ++ InMemoryMonitor.layer
}
