package com.keylesspalace.tusky

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import autodispose2.androidx.lifecycle.autoDispose
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.components.timeline.TimelineFragment
import com.keylesspalace.tusky.components.timeline.TimelineViewModel
import com.keylesspalace.tusky.databinding.ActivityModalTimelineBinding
import com.keylesspalace.tusky.di.ViewModelFactory
import com.keylesspalace.tusky.interfaces.ActionButtonActivity
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import net.accelf.yuito.QuickTootViewModel
import javax.inject.Inject

class ModalTimelineActivity : BottomSheetActivity(), ActionButtonActivity, HasAndroidInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>
    @Inject
    lateinit var eventHub: EventHub
    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private val quickTootViewModel: QuickTootViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityModalTimelineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.includedToolbar.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.title_list_timeline)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        if (supportFragmentManager.findFragmentById(R.id.contentFrame) == null) {
            val kind = intent?.getSerializableExtra(ARG_KIND) as? TimelineViewModel.Kind
                ?: TimelineViewModel.Kind.HOME
            val argument = intent?.getStringExtra(ARG_ARG)
            supportFragmentManager.beginTransaction()
                .replace(R.id.contentFrame, TimelineFragment.newInstance(kind, argument))
                .commit()
        }

        binding.viewQuickToot.attachViewModel(quickTootViewModel, this)

        eventHub.events
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this, Lifecycle.Event.ON_DESTROY)
            .subscribe(binding.viewQuickToot::handleEvent)
        binding.floatingBtn.setOnClickListener(binding.viewQuickToot::onFABClicked)
    }

    override fun getActionButton(): FloatingActionButton? = null

    override fun androidInjector() = dispatchingAndroidInjector

    companion object {
        private const val ARG_KIND = "kind"
        private const val ARG_ARG = "arg"

        @JvmStatic
        fun newIntent(
            context: Context,
            kind: TimelineViewModel.Kind,
            argument: String?
        ): Intent {
            val intent = Intent(context, ModalTimelineActivity::class.java)
            intent.putExtra(ARG_KIND, kind)
            intent.putExtra(ARG_ARG, argument)
            return intent
        }
    }
}
