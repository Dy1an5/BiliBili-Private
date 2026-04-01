package com.example.bilidynamic1.ui.dialog

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import com.example.bilidynamic1.R
import com.example.bilidynamic1.data.model.DynamicItem
import com.example.bilidynamic1.data.model.Group
import com.example.bilidynamic1.data.repository.GroupRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class GroupSelectionDialog(
    private val context: Context,
    private val dynamicItem: DynamicItem,
    private val groupRepository: GroupRepository,
    private val onGroupsUpdated: () -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun show(anchorView: View) {
        // 1. 加载最新的高级感布局
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_group_selection_new, null)
        val listView = view.findViewById<ListView>(R.id.listGroups)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)

        // 💡 关键修复 1：开启裁剪逻辑，防止滑动时内容冲出 28dp 圆角边缘
        view.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        view.clipToOutline = true

        // 💡 关键修复 2：设置固定且修长的尺寸 (宽240dp，高占屏幕42%)
        val density = context.resources.displayMetrics.density
        val displayMetrics = context.resources.displayMetrics
        val widthPx = (240 * density).toInt()
        val heightPx = (displayMetrics.heightPixels * 0.42).toInt()

        val popupWindow = PopupWindow(view, widthPx, heightPx, true).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 50f
            animationStyle = android.R.style.Animation_Dialog
        }

        // 按钮点击：取消
        btnCancel.setOnClickListener { popupWindow.dismiss() }

        scope.launch {
            // 加载数据
            val groups = withContext(Dispatchers.IO) {
                groupRepository.getAllGroups().first()
            }

            val adapter = GroupSelectionAdapter(context, groups, listView)
            listView.adapter = adapter
            listView.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE

            // 初始化选中状态
            groups.forEachIndexed { index, group ->
                if (group.upIds.contains(dynamicItem.uid)) {
                    listView.setItemChecked(index, true)
                }
            }
            adapter.notifyDataSetChanged()

            // 点击同步：点击任意行即刷新适配器，更新选中背景
            listView.setOnItemClickListener { _, _, _, _ ->
                adapter.notifyDataSetChanged()
            }

            // 确定按钮：保存修改
            btnConfirm.setOnClickListener {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        groups.forEachIndexed { index, group ->
                            val isChecked = listView.isItemChecked(index)
                            val currentlyHas = group.upIds.contains(dynamicItem.uid)

                            if (isChecked && !currentlyHas) {
                                val newIds = group.upIds.toMutableSet().apply { add(dynamicItem.uid) }
                                groupRepository.updateGroup(group.copy(upIds = newIds))
                            } else if (!isChecked && currentlyHas) {
                                val newIds = group.upIds.toMutableSet().apply { remove(dynamicItem.uid) }
                                groupRepository.updateGroup(group.copy(upIds = newIds))
                            }
                        }
                    }
                    onGroupsUpdated()
                    popupWindow.dismiss()
                }
            }
        }

        // 居中弹出
        popupWindow.showAtLocation(anchorView, Gravity.CENTER, 0, 0)
        dimBehind(popupWindow)
    }

    // --- 💡 内部适配器：处理风格统一的选中效果 ---
    private class GroupSelectionAdapter(
        context: Context,
        groups: List<Group>,
        private val listView: ListView
    ) : ArrayAdapter<Group>(context, 0, groups) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_group_select_row, parent, false)

            val group = getItem(position)!!
            val nameText = v.findViewById<TextView>(R.id.tvGroupName)
            val colorDot = v.findViewById<View>(R.id.viewColorDot)
            val checkBox = v.findViewById<CheckBox>(R.id.groupCheckBox)

            nameText.text = group.name
            colorDot.backgroundTintList = ColorStateList.valueOf(group.color)

            val isChecked = listView.isItemChecked(position)
            checkBox.isChecked = isChecked

            // 💡 关键修改：同步选中后的视觉效果
            if (isChecked) {
                // 选中状态：浅灰圆角背景，纯黑粗体字
                v.setBackgroundResource(R.drawable.bg_item_selected)
                nameText.setTextColor(Color.parseColor("#1A1A1A")) // 纯黑
                nameText.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                // 未选中状态：透明背景，普通黑字体
                v.setBackgroundColor(Color.TRANSPARENT)
                nameText.setTextColor(Color.parseColor("#444444")) // 稍微淡一点的黑
                nameText.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            return v
        }
    }

    private fun dimBehind(popupWindow: PopupWindow) {
        val container = popupWindow.contentView.rootView
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val p = container.layoutParams as WindowManager.LayoutParams
        p.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        p.dimAmount = 0.5f
        wm.updateViewLayout(container, p)
    }
}