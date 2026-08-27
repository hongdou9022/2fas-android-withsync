package com.twofasapp.feature.home.ui.groups

import androidx.lifecycle.ViewModel
import com.twofasapp.common.domain.Service
import com.twofasapp.common.ktx.launchScoped
import com.twofasapp.data.services.GroupsRepository
import com.twofasapp.data.services.ServicesRepository
import com.twofasapp.data.services.domain.Group
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

internal data class GroupsUiState(
    val groups: List<Group> = emptyList(),
    val services: List<Service> = emptyList(),
    val isLoading: Boolean = true,
    val isSavingEntries: Boolean = false,
)

internal class GroupsViewModel(
    private val groupsRepository: GroupsRepository,
    private val servicesRepository: ServicesRepository,
) : ViewModel() {

    val uiState = MutableStateFlow(GroupsUiState())

    init {
        launchScoped {
            combine(
                groupsRepository.observeGroups(),
                servicesRepository.observeServices(),
            ) { groups, services -> groups to services }
                .collect { (groups, services) ->
                    uiState.update {
                        it.copy(
                            groups = groups,
                            services = services,
                            isLoading = false,
                        )
                    }
                }
        }
    }

    fun addGroup(name: String) {
        launchScoped { groupsRepository.addGroup(name) }
    }

    fun changeGroupName(id: String, name: String) {
        launchScoped { groupsRepository.editGroup(id, name) }
    }

    fun deleteGroup(id: String) {
        launchScoped { groupsRepository.deleteGroup(id) }
    }

    fun assignGroupEntries(
        groupId: String,
        selectedServiceIds: Set<Long>,
        onSaved: () -> Unit,
    ) {
        if (uiState.value.isSavingEntries) return

        launchScoped {
            uiState.update { it.copy(isSavingEntries = true) }

            val result = runCatching {
                val assignments = servicesRepository.getServices()
                    .mapNotNull { service ->
                        when {
                            service.id in selectedServiceIds && service.groupId != groupId -> service.id to groupId
                            service.id !in selectedServiceIds && service.groupId == groupId -> service.id to null
                            else -> null
                        }
                    }
                    .toMap()

                servicesRepository.setServiceGroups(assignments)
            }

            uiState.update { it.copy(isSavingEntries = false) }
            if (result.isSuccess) onSaved()
        }
    }
}
