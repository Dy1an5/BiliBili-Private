package com.example.bilidynamic1.ui.fragment

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.repository.DynamicRepository
import com.example.bilidynamic1.data.repository.GroupRepository
import com.example.bilidynamic1.ui.activity.VideoPlayerActivity
import com.example.bilidynamic1.data.model.DynamicItem
import com.example.bilidynamic1.data.model.Group
import com.example.bilidynamic1.ui.dialog.GroupSelectionDialog
import com.example.bilidynamic1.ui.adapter.DynamicAdapter
import com.example.bilidynamic1.ui.adapter.PopupGroupAdapter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DynamicFragment : Fragment(R.layout.fragment_dynamic) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var tvCollapsedTitle: TextView
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var dividerLine: View
    private lateinit var fabGroup: FloatingActionButton

    private lateinit var groupRepository: GroupRepository
    private var allDynamicItems = mutableListOf<DynamicItem>()
    private var currentGroup: Group? = null // 当前选中的分组

    // 初始化 Adapter，处理点击和长按
    private val adapter by lazy {
        DynamicAdapter(
            onItemClick = { item -> onDynamicClick(item) },
            onItemLongClick = { item, view ->
                showGroupSelectionDialog(item, view)
            }
        )
    }

    private var currentOffset = ""
    private var isRefreshing = false
    private var isLoadingMore = false
    private var hasMoreData = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        groupRepository = GroupRepository(requireContext())

        initViews(view)
        setupTitle()
        setupRecyclerView()
        setupSwipeRefresh()

        loadFirstPage()
    }

    private fun initViews(view: View) {
        collapsingToolbar = view.findViewById(R.id.collapsingToolbar)
        tvCollapsedTitle = view.findViewById(R.id.tvCollapsedTitle)
        appBarLayout = view.findViewById(R.id.appBarLayout)
        dividerLine = view.findViewById(R.id.dividerLine)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        recyclerView = view.findViewById(R.id.recyclerViewDynamic)

        fabGroup = view.findViewById(R.id.fabGroup)
        fabGroup.setOnClickListener { showGroupManagerDialog() }
    }

    /**
     * 弹出管理面板 (dialog_group_manager)
     */
    private fun showGroupManagerDialog() {
        // 1. 加载最新的高级感布局
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_group_manager, null)
        // 💡 关键修复：开启裁剪逻辑，防止滑动时内容超出卡片的 28dp 圆角
        popupView.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        popupView.clipToOutline = true

        val density = resources.displayMetrics.density
        val displayMetrics = resources.displayMetrics

        // 💡 调整为更精致的修长比例
        val widthPx = (220 * density).toInt()  // 宽度调回 220dp
        val heightPx = (displayMetrics.heightPixels * 0.38).toInt() // 高度降为 38%

        val popupWindow = PopupWindow(
            popupView,
            widthPx,
            heightPx,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 40f
            animationStyle = android.R.style.Animation_Dialog
        }

        // 3. 【逻辑绑定：全局模式切换】
        val tvMode = popupView.findViewById<TextView>(R.id.tvCurrentMode)
        fun updateModeUI() {
            val isWhitelist = groupRepository.isGlobalWhitelist()
            tvMode.text = if (isWhitelist) "当前：只看选中UP" else "当前：屏蔽选中UP"
            // 白名单用靛蓝，黑名单用深红，体现高级感区分
            tvMode.setTextColor(Color.parseColor(if (isWhitelist) "#5856D6" else "#FF3B30"))
        }
        updateModeUI() // 初始状态显示

        tvMode.setOnClickListener {
            // 切换数据库状态
            groupRepository.setGlobalFilterMode(!groupRepository.isGlobalWhitelist())
            updateModeUI() // 更新弹窗文字
            filterDynamicsByGroup() // ⬅️ 核心：立即触发背景动态列表刷新
        }

        // 4. 【逻辑绑定：分组列表滑动区域】
        val rv = popupView.findViewById<RecyclerView>(R.id.rvGroupList)
        rv.layoutManager = LinearLayoutManager(context)

        // 监听数据库变化，更新弹窗内的列表
        lifecycleScope.launch {
            groupRepository.getAllGroups().collectLatest { groups ->
                val items = mutableListOf<Group?>(null).apply { addAll(groups) }

                // 确保适配器参数正确：数据, 当前选中组, 选择回调, 删除回调
                rv.adapter = PopupGroupAdapter(items, currentGroup, { selected ->
                    currentGroup = selected
                    filterDynamicsByGroup() // 执行筛选
                    popupWindow.dismiss()    // 选中后自动关闭
                }, { toDelete ->
                    // 删除逻辑
                    lifecycleScope.launch { groupRepository.deleteGroup(toDelete) }
                })
            }
        }

        // 5. 【逻辑绑定：新建分组按钮】
        val btnNew = popupView.findViewById<Button>(R.id.btnNewGroup)
        btnNew.setOnClickListener {
            popupWindow.dismiss() // 先关掉管理弹窗
            showCreateGroupDialog() // ⬅️ 核心：跳转到新建弹窗
        }

        // 6. 【物理位置计算】显示在悬浮按钮的左侧
        val location = IntArray(2)
        fabGroup.getLocationOnScreen(location)

        val fabX = location[0]        // 悬浮按钮左边缘的 X 坐标
        val fabY = location[1]        // 悬浮按钮顶部的 Y 坐标
        val fabHeight = fabGroup.height

// 💡 关键修改点：增加间距，让它离按钮远一点（靠左）
// 将原来的 (12 * density) 改为 (24 * density)
        val horizontalSpacing = (24 * density).toInt()

// 计算 X：按钮左边缘 - 弹窗宽度 - 间距
        val xOffset = fabX - widthPx - horizontalSpacing

// 计算 Y：使弹窗底部与按钮底部对齐（保持不变）
        val yOffset = (fabY + fabHeight) - heightPx

// 保底逻辑：防止弹窗被挤出屏幕左边缘
// 这里的 16dp 是屏幕左边框的最小留白
        val finalX = xOffset.coerceAtLeast((16 * density).toInt())
        val finalY = yOffset.coerceAtLeast((16 * density).toInt())

        popupWindow.showAtLocation(fabGroup, Gravity.NO_GRAVITY, finalX, finalY)
        dimBehind(popupWindow)
    }

    private fun filterDynamicsByGroup() {
        val isGlobalWhitelist = groupRepository.isGlobalWhitelist()
        val group = currentGroup

        val resultList = if (group == null) {
            fabGroup.setImageResource(R.drawable.ic_group_all)
            fabGroup.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#000000"))
            allDynamicItems.toList()
        } else {
            fabGroup.setImageResource(R.drawable.ic_filter)
            fabGroup.backgroundTintList = ColorStateList.valueOf(group.color)
            allDynamicItems.filter { item ->
                val isContained = group.upIds.contains(item.uid)
                if (isGlobalWhitelist) isContained else !isContained
            }
        }

        adapter.submitList(resultList) {
            if (group != null && resultList.size < 6 && hasMoreData && !isLoadingMore && !isRefreshing) {
                loadData(false)
            }
        }
    }

    private fun loadData(isRefresh: Boolean) {
        if (isRefresh) {
            if (isRefreshing) return
            isRefreshing = true
            swipeRefresh.isRefreshing = true
            currentOffset = ""
        } else {
            if (isLoadingMore || !hasMoreData) return
            isLoadingMore = true
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = DynamicRepository.loadVideoDynamicsPage(currentOffset)
                withContext(Dispatchers.Main) {
                    if (isRefresh) {
                        allDynamicItems.clear()
                    }
                    allDynamicItems.addAll(result.items)
                    currentOffset = result.nextOffset
                    hasMoreData = result.hasMore
                    filterDynamicsByGroup()

                    isRefreshing = false
                    isLoadingMore = false
                    swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isRefreshing = false
                    isLoadingMore = false
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(context, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lm = recyclerView.layoutManager as LinearLayoutManager
                if (!isLoadingMore && hasMoreData && lm.findLastVisibleItemPosition() >= lm.itemCount - 3) {
                    loadData(false)
                }
            }
        })
    }

    private fun showGroupSelectionDialog(item: DynamicItem, anchorView: View) {
        GroupSelectionDialog(requireContext(), item, groupRepository) {
            filterDynamicsByGroup()
        }.show(anchorView)
    }

    private fun showCreateGroupDialog() {
        val activityWindow = requireActivity().window
        val originalMode = activityWindow.attributes.softInputMode

        // 1. 💡 放弃系统全屏主题，使用默认主题（或自定义圆角主题），避免焦点被拦截
        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedCornerDialog)
            .create()

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_group_responsive, null)
        val etGroupName = dialogView.findViewById<EditText>(R.id.etGroupName)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val premiumCard = dialogView.findViewById<View>(R.id.premiumCard)
        val outerContainer = dialogView.findViewById<View>(R.id.outerContainer)

        // 💡 关键 1：锁死宿主 Activity 背景
        activityWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        dialog.show()
        dialog.setContentView(dialogView)

        // 2. 💡 在 show 之后手动把窗口设为全屏并透明
        dialog.window?.apply {
            // 强制占满屏幕，确保 ConstraintLayout 的居中逻辑生效
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // 💡 关键修复：允许 Dialog 调整大小（这样它能感知键盘高度变化），但由于背景 Activity 已经锁定，背景不会动
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

            // 确保清除所有可能阻塞输入的标志
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.5f)
        }

        // 3. 💡 强力唤起键盘
        etGroupName.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            // 延迟触发，给 Window 获取焦点的时间
            postDelayed({
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }, 300)
        }

        // 4. 平移逻辑 (保持不变)
        ViewCompat.setOnApplyWindowInsetsListener(outerContainer) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val translationY = if (imeHeight > 0) -(imeHeight / 2).toFloat() else 0f
            premiumCard.animate().translationY(translationY).setDuration(200).start()
            insets
        }

        // 逻辑绑定
        outerContainer.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener { activityWindow.setSoftInputMode(originalMode) }

        btnConfirm.setOnClickListener {
            val name = etGroupName.text.toString().trim()
            if (name.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    groupRepository.createGroup(name)
                    dialog.dismiss()
                    fabGroup.postDelayed({ showGroupManagerDialog() }, 300)
                }
            }
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
    }

    private fun setupAppBarListener() {
        appBarLayout.addOnOffsetChangedListener { appBar, offset ->
            val fraction = Math.abs(offset).toFloat() / appBar.totalScrollRange
            tvCollapsedTitle.visibility = if (fraction > 0.8f) View.VISIBLE else View.GONE
            dividerLine.visibility = if (fraction > 0.8f) View.GONE else View.VISIBLE
        }
    }

    private fun setupTitle() {
        collapsingToolbar.title = "动态"
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener { loadData(true) }
    }

    private fun loadFirstPage() = loadData(true)

    private fun onDynamicClick(item: DynamicItem) {
        if (item.bvid.isEmpty()) return
        if (item.cid != 0L) {
            goToPlayer(item.bvid, item.cid)
        } else {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val realCid = DynamicRepository.fetchCid(item.bvid)
                withContext(Dispatchers.Main) {
                    if (realCid > 0) goToPlayer(item.bvid, realCid)
                }
            }
        }
    }

    private fun goToPlayer(bvid: String, cid: Long) {
        val intent = Intent(requireContext(), VideoPlayerActivity::class.java).apply {
            putExtra("bvid", bvid)
            putExtra("cid", cid)
        }
        startActivity(intent)
    }

    // 💡 通用的背景变暗处理函数
    private fun dimBehind(popupWindow: PopupWindow) {
        val container = popupWindow.contentView.rootView
        val context = popupWindow.contentView.context
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val p = container.layoutParams as WindowManager.LayoutParams

        // 设置变暗标志位
        p.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        p.dimAmount = 0.5f // 💡 0.5f 是标准阴影浓度，数值越大背景越黑

        wm.updateViewLayout(container, p)
    }
}