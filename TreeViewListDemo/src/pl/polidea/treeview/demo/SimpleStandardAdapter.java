package pl.polidea.treeview.demo;

import java.util.Arrays;
import java.util.Set;

import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.R;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This is a very simple adapter that provides very basic tree view with a
 * checkboxes and simple item description.
 * 
 */
class SimpleStandardAdapter extends AbstractTreeViewAdapter<Long> {

    private final Set<Long> selected;

    private final OnCheckedChangeListener onCheckedChange = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final CompoundButton buttonView,
                final boolean isChecked) {
            final Long id = (Long) buttonView.getTag();
            changeSelected(isChecked, id);
        }

    };

    private void changeSelected(final boolean isChecked, final Long id) {
        if (isChecked) {
            selected.add(id);
        } else {
            selected.remove(id);
        }
    }

    public SimpleStandardAdapter(final TreeViewListDemo treeViewListDemo,
            final Set<Long> selected,
            final TreeStateManager<Long> treeStateManager,
            final int numberOfLevels) {
        super(treeViewListDemo, treeStateManager, numberOfLevels);
        this.selected = selected;
    }

    private String getDescription(final long id) {
       /* final Integer[] hierarchy = getManager().getHierarchyDescription(id);
        return "Node " + id + Arrays.asList(hierarchy);*/
        return "Node " + id;
    }

    @Override
    public View getNewChildView(final TreeNodeInfo<Long> treeNodeInfo) {
        final LinearLayout viewLayout = (LinearLayout) getActivity()
                .getLayoutInflater().inflate(R.layout.demo_list_item, null);
        return updateView(viewLayout, treeNodeInfo);
    }

    @Override
    public LinearLayout updateView(final View view,
            final TreeNodeInfo<Long> treeNodeInfo) {
        final LinearLayout viewLayout = (LinearLayout) view;
        final TextView descriptionView = (TextView) viewLayout
                .findViewById(R.id.demo_list_item_description);
        final TextView levelView = (TextView) viewLayout
                .findViewById(R.id.demo_list_item_level);
        descriptionView.setText(getDescription(treeNodeInfo.getId()));
        levelView.setTextColor(Color.RED);
        SharedPreferences sharedPreferences=this.getActivity().getSharedPreferences("PREFERENCENAME", Context.MODE_MULTI_PROCESS);
        String mainTreeEtvValue =sharedPreferences.getString("mainTreeEtvValue", "0");
        int mainTreeEtvValueInt = Integer.parseInt(mainTreeEtvValue);
        
        if(mainTreeEtvValueInt<=getCount() && treeNodeInfo.getId()==mainTreeEtvValueInt){
            descriptionView.setTextColor(Color.BLUE);
        }
        else {
            descriptionView.setTextColor(Color.GREEN);
        }
        levelView.setText(Integer.toString(treeNodeInfo.getLevel()));
        final CheckBox box = (CheckBox) viewLayout
                .findViewById(R.id.demo_list_checkbox);
        box.setTag(treeNodeInfo.getId());
        if (treeNodeInfo.isWithChildren()) {
            box.setVisibility(View.GONE);
        } else {
            box.setVisibility(View.VISIBLE);
            box.setChecked(selected.contains(treeNodeInfo.getId()));
        }
        box.setOnCheckedChangeListener(onCheckedChange);
        notifyDataSetChanged();
        return viewLayout;
    }

    @Override
    public void handleItemClick(final View view, final Object id) {
        final Long longId = (Long) id;
        final TreeNodeInfo<Long> info = getManager().getNodeInfo(longId);
        if (info.isWithChildren()) {
            super.handleItemClick(view, id);
        } else {
            final ViewGroup vg = (ViewGroup) view;
            final CheckBox cb = (CheckBox) vg
                    .findViewById(R.id.demo_list_checkbox);
            cb.performClick();
        }
    }

    @Override
    public long getItemId(final int position) {
        return getTreeId(position);
    }
    
    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return super.getCount();
    }
}