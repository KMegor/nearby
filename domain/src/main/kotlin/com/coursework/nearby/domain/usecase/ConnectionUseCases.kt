package com.coursework.nearby.domain.usecase

import com.coursework.nearby.domain.model.ConnectionLists
import com.coursework.nearby.domain.model.RequestResponse
import com.coursework.nearby.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveConnectionsUseCase @Inject constructor(private val connections: ConnectionRepository) {
    operator fun invoke(): Flow<ConnectionLists> = connections.connections.map(ConnectionLists::from)
}

class RefreshConnectionsUseCase @Inject constructor(private val connections: ConnectionRepository) {
    suspend operator fun invoke() = connections.refresh()
}

class RespondToRequestUseCase @Inject constructor(private val connections: ConnectionRepository) {
    suspend operator fun invoke(connectionId: String, response: RequestResponse) =
        connections.respond(connectionId, response)
}

class UnblockUseCase @Inject constructor(private val connections: ConnectionRepository) {
    suspend operator fun invoke(connectionId: String) = connections.unblock(connectionId)
}
