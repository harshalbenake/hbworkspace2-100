package pl.polidea.treeview.demo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.R;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Demo activity showing how the tree view can be used.
 * 
 */
public class TreeViewListDemo extends Activity {
    private enum TreeType implements Serializable {
        SIMPLE,
        FANCY
    }

    private final Set<Long> selected = new HashSet<Long>();

    private static final String TAG = TreeViewListDemo.class.getSimpleName();
    private TreeViewList treeView;

    private static final int[] DEMO_NODES = new int[] { 0, 0, 1, 1, 1, 2, 2, 1,
            1, 2, 1, 0, 0, 0, 1, 2, 3,4,5,6, 2, 0, 0, 1, 2, 0, 1, 2, 0, 1 };
    private static final int LEVEL_NUMBER = 7;
    private TreeStateManager<Long> manager = null;
    private FancyColouredVariousSizesAdapter fancyAdapter;
    private SimpleStandardAdapter simpleAdapter;
    private TreeType treeType;
    private boolean collapsible;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TreeType newTreeType = null;
        boolean newCollapsible;
        if (savedInstanceState == null) {
            manager = new InMemoryTreeStateManager<Long>();
            final TreeBuilder<Long> treeBuilder = new TreeBuilder<Long>(manager);
            for (int i = 0; i < DEMO_NODES.length; i++) {
                treeBuilder.sequentiallyAddNextNode((long) i, DEMO_NODES[i]);
            }
            Log.d(TAG, manager.toString());
            newTreeType = TreeType.SIMPLE;
            newCollapsible = true;
        } else {
            manager = (TreeStateManager<Long>) savedInstanceState
                    .getSerializable("treeManager");
            if (manager == null) {
                manager = new InMemoryTreeStateManager<Long>();
            }
            newTreeType = (TreeType) savedInstanceState
                    .getSerializable("treeType");
            if (newTreeType == null) {
                newTreeType = TreeType.SIMPLE;
            }
            newCollapsible = savedInstanceState.getBoolean("collapsible");
        }
        setContentView(R.layout.main_demo);
        treeView = (TreeViewList) findViewById(R.id.mainTreeView);
        fancyAdapter = new FancyColouredVariousSizesAdapter(this, selected,
                manager, LEVEL_NUMBER);
        simpleAdapter = new SimpleStandardAdapter(this, selected, manager,
                LEVEL_NUMBER);
        setTreeAdapter(newTreeType);
        setCollapsible(newCollapsible);
        registerForContextMenu(treeView);
        
        final EditText mainTreeEtv=(EditText)findViewById(R.id.mainTreeEtv);

        final Button mainTreeBtn=(Button)findViewById(R.id.mainTreeBtn);
        mainTreeBtn.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences=getSharedPreferences("PREFERENCENAME", Context.MODE_MULTI_PROCESS);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                String mainTreeEtvValue = mainTreeEtv.getText().toString();
                editor.putString("mainTreeEtvValue", mainTreeEtvValue);
                editor.commit();
                treeView.invalidateViews();
            }
        });
        
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        outState.putSerializable("treeManager", manager);
        outState.putSerializable("treeType", treeType);
        outState.putBoolean("collapsible", this.collapsible);
        super.onSaveInstanceState(outState);
    }

    protected final void setTreeAdapter(final TreeType newTreeType) {
        this.treeType = newTreeType;
        switch (newTreeType) {
        case SIMPLE:
            treeView.setAdapter(simpleAdapter);
            break;
        case FANCY:
            treeView.setAdapter(fancyAdapter);
            break;
        default:
            treeView.setAdapter(simpleAdapter);
        }
    }

    protected final void setCollapsible(final boolean newCollapsible) {
        this.collapsible = newCollapsible;
        treeView.setCollapsible(this.collapsible);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        final MenuItem collapsibleMenu = menu
                .findItem(R.id.collapsible_menu_item);
        if (collapsible) {
            collapsibleMenu.setTitle(R.string.collapsible_menu_disable);
            collapsibleMenu.setTitleCondensed(getResources().getString(
                    R.string.collapsible_condensed_disable));
        } else {
            collapsibleMenu.setTitle(R.string.collapsible_menu_enable);
            collapsibleMenu.setTitleCondensed(getResources().getString(
                    R.string.collapsible_condensed_enable));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.simple_menu_item) {
            setTreeAdapter(TreeType.SIMPLE);
        } else if (item.getItemId() == R.id.fancy_menu_item) {
            setTreeAdapter(TreeType.FANCY);
        } else if (item.getItemId() == R.id.collapsible_menu_item) {
            setCollapsible(!this.collapsible);
        } else if (item.getItemId() == R.id.expand_all_menu_item) {
            manager.expandEverythingBelow(null);
        } else if (item.getItemId() == R.id.collapse_all_menu_item) {
            manager.collapseChildren(null);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenuInfo menuInfo) {
        final AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo) menuInfo;
        final long id = adapterInfo.id;
        final TreeNodeInfo<Long> info = manager.getNodeInfo(id);
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.context_menu, menu);
        if (info.isWithChildren()) {
            if (info.isExpanded()) {
                menu.findItem(R.id.context_menu_expand_item).setVisible(false);
                menu.findItem(R.id.context_menu_expand_all).setVisible(false);
            } else {
                menu.findItem(R.id.context_menu_collapse).setVisible(false);
            }
        } else {
            menu.findItem(R.id.context_menu_expand_item).setVisible(false);
            menu.findItem(R.id.context_menu_expand_all).setVisible(false);
            menu.findItem(R.id.context_menu_collapse).setVisible(false);
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        final long id = info.id;
        if (item.getItemId() == R.id.context_menu_collapse) {
            manager.collapseChildren(id);
            return true;
        } else if (item.getItemId() == R.id.context_menu_expand_all) {
            manager.expandEverythingBelow(id);
            return true;
        } else if (item.getItemId() == R.id.context_menu_expand_item) {
            manager.expandDirectChildren(id);
            return true;
        } else if (item.getItemId() == R.id.context_menu_delete) {
            manager.removeNodeRecursively(id);
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }
}