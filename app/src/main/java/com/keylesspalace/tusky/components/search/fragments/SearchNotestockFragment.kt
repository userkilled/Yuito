package com.keylesspalace.tusky.components.search.fragments

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.keylesspalace.tusky.components.search.adapter.SearchStatusesAdapter
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.util.NetworkState
import com.keylesspalace.tusky.viewdata.StatusViewData

class SearchNotestockFragment : SearchStatusesFragment() {

    override val networkStateRefresh: LiveData<NetworkState>
        get() = viewModel.networkStateNotestockRefresh
    override val networkState: LiveData<NetworkState>
        get() = viewModel.networkStateNotestock
    override val data: LiveData<PagedList<Pair<Status, StatusViewData.Concrete>>>
        get() = viewModel.notestockStatuses

    override fun onContentHiddenChange(isShowing: Boolean, position: Int) {
        (adapter as? SearchStatusesAdapter)?.getItem(position)?.let {
            viewModel.contentHiddenNotestockChange(it, isShowing)
        }
    }

    override fun onFavourite(favourite: Boolean, position: Int) {
        // Forbidden
    }

    override fun onViewThread(position: Int) {
        (adapter as? SearchStatusesAdapter)?.getItem(position)?.first?.let { status ->
            val actionableStatus = status.actionableStatus
            bottomSheetActivity?.viewUrl(actionableStatus.id, actionableStatus.id)
        }
    }

    override fun onExpandedChange(expanded: Boolean, position: Int) {
        (adapter as? SearchStatusesAdapter)?.getItem(position)?.let {
            viewModel.expandedNotestockChange(it, expanded)
        }
    }

    override fun onContentCollapsedChange(isCollapsed: Boolean, position: Int) {
        (adapter as? SearchStatusesAdapter)?.getItem(position)?.let {
            viewModel.collapsedNotestockChange(it, isCollapsed)
        }
    }

    override fun onVoteInPoll(position: Int, choices: MutableList<Int>) {
        // Forbidden
    }

    override fun removeItem(position: Int) {
        (adapter as? SearchStatusesAdapter)?.getItem(position)?.let {
            viewModel.removeNotestockItem(it)
        }
    }

    override fun onReblog(reblog: Boolean, position: Int) {
        // Forbidden
    }

    companion object {
        fun newInstance() = SearchNotestockFragment()
    }

}