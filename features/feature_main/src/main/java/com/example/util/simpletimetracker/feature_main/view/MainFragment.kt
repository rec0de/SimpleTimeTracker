package com.example.util.simpletimetracker.feature_main.view

import android.graphics.ColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.AttrRes
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.example.util.simpletimetracker.core.base.BaseFragment
import com.example.util.simpletimetracker.core.di.BaseViewModelFactory
import com.example.util.simpletimetracker.core.extension.addOnPageChangeCallback
import com.example.util.simpletimetracker.core.sharedViewModel.MainTabsViewModel
import com.example.util.simpletimetracker.core.utils.SHORTCUT_NAVIGATION_KEY
import com.example.util.simpletimetracker.core.view.SafeFragmentStateAdapter
import com.example.util.simpletimetracker.domain.extension.orZero
import com.example.util.simpletimetracker.feature_base_adapter.extensions.getThemedAttr
import com.example.util.simpletimetracker.feature_main.R
import com.example.util.simpletimetracker.feature_main.adapter.MainContentAdapter
import com.example.util.simpletimetracker.feature_main.provider.MainTabsProvider
import com.example.util.simpletimetracker.feature_main.viewModel.MainViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.util.simpletimetracker.feature_main.databinding.MainFragmentBinding as Binding

@AndroidEntryPoint
class MainFragment : BaseFragment<Binding>() {

    override val inflater: (LayoutInflater, ViewGroup?, Boolean) -> Binding =
        Binding::inflate

    @Inject
    lateinit var mainTabsViewModelFactory: BaseViewModelFactory<MainTabsViewModel>

    @Inject
    lateinit var mainTabsProvider: MainTabsProvider

    private val viewModel: MainViewModel by viewModels()
    private val mainTabsViewModel: MainTabsViewModel by activityViewModels(
        factoryProducer = { mainTabsViewModelFactory },
    )

    private val selectedColorFilter by lazy { getColorFilter(R.attr.appTabSelectedColor) }
    private val unselectedColorFilter by lazy { getColorFilter(R.attr.appTabUnselectedColor) }
    private val backPressedCallback: OnBackPressedCallback = getOnBackPressedCallback()
    private var shortcutNavigationHandled = false
    private val mainPagePosition by lazy {
        mainTabsProvider.mainTab.let(mainTabsProvider::mapTabToPosition)
    }

    override fun initUi() {
        setupPager()
        checkForShortcutNavigation()
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    override fun initViewModel() {
        viewModel.initialize
    }

    private fun setupPager() = with(binding) {
        mainPager.adapter = SafeFragmentStateAdapter(
            MainContentAdapter(
                fragment = this@MainFragment,
                tabs = mainTabsProvider.tabsList,
            ),
        )
        mainPager.offscreenPageLimit = 3 // Same as number of pages to avoid recreating.
        mainPager.addOnPageChangeCallback(lifecycleOwner = this@MainFragment) { state ->
            mainTabsViewModel.onScrollStateChanged(
                isScrolling = state != ViewPager2.SCROLL_STATE_IDLE,
            )
        }

        TabLayoutMediator(mainTabs, mainPager) { tab, position ->
            position.let(mainTabsProvider::mapPositionToIcon).let(tab::setIcon)
            tab.icon?.colorFilter = if (position == mainPagePosition) {
                selectedColorFilter
            } else {
                unselectedColorFilter
            }
        }.attach()

        mainTabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    tab?.position
                        ?.let(mainTabsProvider::mapPositionToTab)
                        ?.let(mainTabsViewModel::onTabReselected)
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    tab?.icon?.colorFilter = unselectedColorFilter
                }

                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.icon?.colorFilter = selectedColorFilter
                    backPressedCallback.isEnabled = tab?.position.orZero() != mainPagePosition
                }
            },
        )
        mainPager.setCurrentItem(mainPagePosition, false)
    }

    private fun getOnBackPressedCallback(): OnBackPressedCallback {
        return object : OnBackPressedCallback(/* enabled = */ false) {
            override fun handleOnBackPressed() {
                binding.mainPager.setCurrentItem(mainPagePosition, true)
            }
        }
    }

    private fun checkForShortcutNavigation() = with(binding) {
        if (shortcutNavigationHandled) return@with

        activity?.intent?.extras
            ?.getString(SHORTCUT_NAVIGATION_KEY)
            ?.let(mainTabsProvider::mapNavigationToPosition)
            ?.let {
                mainPager.setCurrentItem(it, true)
                shortcutNavigationHandled = true
            }
    }

    private fun getColorFilter(@AttrRes attrRes: Int): ColorFilter? {
        return BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            requireContext().getThemedAttr(attrRes),
            BlendModeCompat.SRC_IN,
        )
    }
}
