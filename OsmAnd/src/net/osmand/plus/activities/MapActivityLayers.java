package net.osmand.plus.activities;


import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import net.osmand.CallbackWithObject;
import net.osmand.ResultMatcher;
import net.osmand.StateChangedListener;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.DownloadedRegionsLayer;
import net.osmand.plus.views.FavoritesLayer;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.ImpassableRoadsLayer;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.MapMarkersLayer;
import net.osmand.plus.views.MapTextLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.plus.views.PointLocationLayer;
import net.osmand.plus.views.PointNavigationLayer;
import net.osmand.plus.views.RouteLayer;
import net.osmand.plus.views.TransportInfoLayer;
import net.osmand.plus.views.TransportStopsLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * Object is responsible to maintain layers using by map activity
 */
public class MapActivityLayers {

	private final MapActivity activity;

	// the order of layer should be preserved ! when you are inserting new layer
	private MapTileLayer mapTileLayer;
	private MapVectorLayer mapVectorLayer;
	private GPXLayer gpxLayer;
	private RouteLayer routeLayer;
	private POIMapLayer poiMapLayer;
	private FavoritesLayer favoritesLayer;
	private TransportStopsLayer transportStopsLayer;
	private TransportInfoLayer transportInfoLayer;
	private PointLocationLayer locationLayer;
	private PointNavigationLayer navigationLayer;
	private MapMarkersLayer mapMarkersLayer;
	private ImpassableRoadsLayer impassableRoadsLayer;
	private MapInfoLayer mapInfoLayer;
	private MapTextLayer mapTextLayer;
	private ContextMenuLayer contextMenuLayer;
	private MapControlsLayer mapControlsLayer;
	private DownloadedRegionsLayer downloadedRegionsLayer;
	private MapWidgetRegistry mapWidgetRegistry;

	private StateChangedListener<Integer> transparencyListener;

	public MapActivityLayers(MapActivity activity) {
		this.activity = activity;
		this.mapWidgetRegistry = new MapWidgetRegistry(activity.getMyApplication().getSettings());
	}

	public MapWidgetRegistry getMapWidgetRegistry() {
		return mapWidgetRegistry;
	}

	public OsmandApplication getApplication() {
		return (OsmandApplication) activity.getApplication();
	}


	public void createLayers(final OsmandMapTileView mapView) {

		OsmandApplication app = getApplication();
		RoutingHelper routingHelper = app.getRoutingHelper();
		// first create to make accessible
		mapTextLayer = new MapTextLayer();
		// 5.95 all labels
		mapView.addLayer(mapTextLayer, 5.95f);
		// 8. context menu layer 
		contextMenuLayer = new ContextMenuLayer(activity);
		mapView.addLayer(contextMenuLayer, 8);
		// mapView.addLayer(underlayLayer, -0.5f);
		mapTileLayer = new MapTileLayer(true);
		mapView.addLayer(mapTileLayer, 0.0f);
		mapView.setMainLayer(mapTileLayer);

		// 0.5 layer
		mapVectorLayer = new MapVectorLayer(mapTileLayer, false);
		mapView.addLayer(mapVectorLayer, 0.5f);

		downloadedRegionsLayer = new DownloadedRegionsLayer();
		mapView.addLayer(downloadedRegionsLayer, 0.5f);

		// 0.9 gpx layer
		gpxLayer = new GPXLayer();
		mapView.addLayer(gpxLayer, 0.9f);

		// 1. route layer
		routeLayer = new RouteLayer(routingHelper);
		mapView.addLayer(routeLayer, 1);

		// 2. osm bugs layer
		// 3. poi layer
		poiMapLayer = new POIMapLayer(activity);
		mapView.addLayer(poiMapLayer, 3);
		// 4. favorites layer
		favoritesLayer = new FavoritesLayer();
		mapView.addLayer(favoritesLayer, 4);
		// 5. transport layer
		transportStopsLayer = new TransportStopsLayer();
		// 5.5 transport info layer 
		transportInfoLayer = new TransportInfoLayer(TransportRouteHelper.getInstance());
		mapView.addLayer(transportInfoLayer, 5.5f);
		// 5.95 all text labels
		// 6. point location layer 
		locationLayer = new PointLocationLayer(activity.getMapViewTrackingUtilities());
		mapView.addLayer(locationLayer, 6);
		// 7. point navigation layer
		navigationLayer = new PointNavigationLayer(activity);
		mapView.addLayer(navigationLayer, 7);
		// 7.3 map markers layer
		mapMarkersLayer = new MapMarkersLayer(activity);
		mapView.addLayer(mapMarkersLayer, 7.3f);
		// 7.5 Impassible roads
		impassableRoadsLayer = new ImpassableRoadsLayer(activity);
		mapView.addLayer(impassableRoadsLayer, 7.5f);
		// 8. context menu layer 
		// 9. map info layer
		mapInfoLayer = new MapInfoLayer(activity, routeLayer);
		mapView.addLayer(mapInfoLayer, 9);
		// 11. route info layer
		mapControlsLayer = new MapControlsLayer(activity);
		mapView.addLayer(mapControlsLayer, 11);

		transparencyListener = new StateChangedListener<Integer>() {
			@Override
			public void stateChanged(Integer change) {
				mapTileLayer.setAlpha(change);
				mapVectorLayer.setAlpha(change);
				mapView.refreshMap();
			}
		};
		app.getSettings().MAP_TRANSPARENCY.addListener(transparencyListener);

		OsmandPlugin.createLayers(mapView, activity);
		app.getAppCustomization().createLayers(mapView, activity);
	}


	public void updateLayers(OsmandMapTileView mapView) {
		OsmandSettings settings = getApplication().getSettings();
		updateMapSource(mapView, settings.MAP_TILE_SOURCES);
		boolean showStops = settings.getCustomRenderBooleanProperty(OsmandSettings.TRANSPORT_STOPS_OVER_MAP).get();
		if (mapView.getLayers().contains(transportStopsLayer) != showStops) {
			if (showStops) {
				mapView.addLayer(transportStopsLayer, 5);
			} else {
				mapView.removeLayer(transportStopsLayer);
			}
		}
		OsmandPlugin.refreshLayers(mapView, activity);
	}

	public void updateMapSource(OsmandMapTileView mapView, CommonPreference<String> settingsToWarnAboutMap) {
		OsmandSettings settings = getApplication().getSettings();

		// update transparency
		int mapTransparency = settings.MAP_UNDERLAY.get() == null ? 255 : settings.MAP_TRANSPARENCY.get();
		mapTileLayer.setAlpha(mapTransparency);
		mapVectorLayer.setAlpha(mapTransparency);

		ITileSource newSource = settings.getMapTileSource(settings.MAP_TILE_SOURCES == settingsToWarnAboutMap);
		ITileSource oldMap = mapTileLayer.getMap();
		if (newSource != oldMap) {
			if (oldMap instanceof SQLiteTileSource) {
				((SQLiteTileSource) oldMap).closeDB();
			}
			mapTileLayer.setMap(newSource);
		}

		boolean vectorData = !settings.MAP_ONLINE_DATA.get();
		mapTileLayer.setVisible(!vectorData);
		mapVectorLayer.setVisible(vectorData);
		if (vectorData) {
			mapView.setMainLayer(mapVectorLayer);
		} else {
			mapView.setMainLayer(mapTileLayer);
		}
	}


	public AlertDialog showGPXFileLayer(List<String> files, final OsmandMapTileView mapView) {
		final OsmandSettings settings = getApplication().getSettings();
		CallbackWithObject<GPXFile[]> callbackWithObject = new CallbackWithObject<GPXFile[]>() {
			@Override
			public boolean processResult(GPXFile[] result) {
				WptPt locToShow = null;
				for (GPXFile g : result) {
					if (g.showCurrentTrack) {
						if (!settings.SAVE_TRACK_TO_GPX.get() && !
								settings.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
							Toast.makeText(activity,
									R.string.gpx_monitoring_disabled_warn, Toast.LENGTH_LONG).show();
						} else {
							g.path = getString(R.string.show_current_gpx_title);
						}
						break;
					} else {
						locToShow = g.findPointToShow();
					}
				}
				getApplication().getSelectedGpxHelper().setGpxFileToDisplay(result);
				if (locToShow != null) {
					mapView.getAnimatedDraggingThread().startMoving(locToShow.lat, locToShow.lon,
							mapView.getZoom(), true);
				}
				mapView.refreshMap();
				activity.getDashboard().refreshContent(true);
				return true;
			}
		};

		if (files == null) {
			return GpxUiHelper.selectGPXFile(activity, true, true, callbackWithObject);
		} else {
			return GpxUiHelper.selectGPXFile(files, activity, true, true, callbackWithObject);
		}
	}


	public void showMultichoicePoiFilterDialog(final OsmandMapTileView mapView, final DismissListener listener) {
		final OsmandApplication app = getApplication();
		final PoiFiltersHelper poiFilters = app.getPoiFilters();
		final ContextMenuAdapter adapter = new ContextMenuAdapter();
		final List<PoiUIFilter> list = new ArrayList<>();
		for (PoiUIFilter f : poiFilters.getTopDefinedPoiFilters()) {
			addFilterToList(adapter, list, f, true);
		}
		for (PoiUIFilter f : poiFilters.getSearchPoiFilters()) {
			addFilterToList(adapter, list, f, true);
		}
		list.add(poiFilters.getCustomPOIFilter());

		final ArrayAdapter<ContextMenuItem> listAdapter = adapter.createListAdapter(activity, app.getSettings().isLightContent());
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		final ListView listView = new ListView(activity);
		listView.setDivider(null);
		listView.setClickable(true);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				ContextMenuItem item = listAdapter.getItem(position);
				item.setSelected(!item.getSelected());
				item.getItemClickListener().onContextMenuClick(listAdapter, position, position, item.getSelected());
				listAdapter.notifyDataSetChanged();
			}
		});
		builder.setView(listView)
				.setTitle(R.string.show_poi_over_map)
				.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						for (int i = 0; i < listAdapter.getCount(); i++) {
							ContextMenuItem item = listAdapter.getItem(i);
								PoiUIFilter filter = list.get(i);
							if (item.getSelected()) {
								getApplication().getPoiFilters().addSelectedPoiFilter(filter);
							} else {
								getApplication().getPoiFilters().removeSelectedPoiFilter(filter);
							}
						}
						mapView.refreshMap();
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				// TODO go to single choice dialog
				.setNeutralButton(" ", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						showSingleChoicePoiFilterDialog(mapView, listener);
					}
				});
		final AlertDialog alertDialog = builder.create();
		alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
				Drawable drawable = app.getIconsCache().getThemedIcon(R.drawable.ic_action_singleselect);
				neutralButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
			}
		});
		alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				listener.dismiss();
			}
		});
		alertDialog.show();
	}

	public void showSingleChoicePoiFilterDialog(final OsmandMapTileView mapView, final DismissListener listener) {
		final OsmandApplication app = getApplication();
		final PoiFiltersHelper poiFilters = app.getPoiFilters();
		final ContextMenuAdapter adapter = new ContextMenuAdapter();
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.shared_string_search, app)
				.setIcon(R.drawable.ic_action_search_dark).createItem());
		final List<PoiUIFilter> list = new ArrayList<>();
		list.add(poiFilters.getCustomPOIFilter());
		for (PoiUIFilter f : poiFilters.getTopDefinedPoiFilters()) {
			addFilterToList(adapter, list, f, false);
		}
		for (PoiUIFilter f : poiFilters.getSearchPoiFilters()) {
			addFilterToList(adapter, list, f, false);
		}

		final ArrayAdapter<ContextMenuItem> listAdapter =
				adapter.createListAdapter(activity, app.getSettings().isLightContent());
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setAdapter(listAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				PoiUIFilter pf = list.get(which);
				String filterId = pf.getFilterId();
				if (filterId.equals(PoiUIFilter.CUSTOM_FILTER_ID)) {
					Intent search = new Intent(activity, SearchActivity.class);
					search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					activity.getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.POI_TAB_INDEX);
					activity.startActivity(search);
				} else {
					getApplication().getPoiFilters().clearSelectedPoiFilters();
					getApplication().getPoiFilters().addSelectedPoiFilter(pf);
					mapView.refreshMap();
				}
			}

		});
		builder.setTitle(R.string.show_poi_over_map);
		builder.setNegativeButton(R.string.shared_string_dismiss, null);
		builder.setNeutralButton(" ", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showMultichoicePoiFilterDialog(mapView, listener);
			}
		});
		final AlertDialog alertDialog = builder.create();
		alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
				Drawable drawable = app.getIconsCache().getThemedIcon(R.drawable.ic_action_multiselect);
				neutralButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
			}
		});
		alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				listener.dismiss();
			}
		});
		alertDialog.show();
	}

	private void addFilterToList(final ContextMenuAdapter adapter,
								 final List<PoiUIFilter> list,
								 final PoiUIFilter f,
								 boolean multichoice) {
		list.add(f);
		ContextMenuItem.ItemBuilder builder = new ContextMenuItem.ItemBuilder();
		if (multichoice) {
			builder.setSelected(getApplication().getPoiFilters().isPoiFilterSelected(f));
			builder.setListener(new ContextMenuAdapter.ItemClickListener() {
				@Override
				public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter,
												  int itemId, int position, boolean isChecked) {
					ContextMenuItem item = adapter.getItem(position);
					item.setSelected(isChecked);
					return false;
				}
			});
		}
		builder.setTitle(f.getName());
		if (RenderingIcons.containsBigIcon(f.getIconId())) {
			builder.setIcon(RenderingIcons.getBigIconResourceId(f.getIconId()));
		} else {
			builder.setIcon(R.drawable.mx_user_defined);
		}
		builder.setColor(ContextMenuItem.INVALID_ID);
		builder.setSkipPaintingWithoutColor(true);
		adapter.addItem(builder.createItem());
	}

	public void selectMapLayer(final OsmandMapTileView mapView) {
		if (OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) == null) {
			Toast.makeText(activity, R.string.map_online_plugin_is_not_installed, Toast.LENGTH_LONG).show();
			return;
		}
		final OsmandSettings settings = getApplication().getSettings();

		final LinkedHashMap<String, String> entriesMap = new LinkedHashMap<>();


		final String layerOsmVector = "LAYER_OSM_VECTOR";
		final String layerInstallMore = "LAYER_INSTALL_MORE";
		final String layerEditInstall = "LAYER_EDIT";

		entriesMap.put(layerOsmVector, getString(R.string.vector_data));
		entriesMap.putAll(settings.getTileSourceEntries());
		entriesMap.put(layerInstallMore, getString(R.string.install_more));
		entriesMap.put(layerEditInstall, getString(R.string.maps_define_edit));

		final List<Entry<String, String>> entriesMapList = new ArrayList<>(entriesMap.entrySet());

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		String selectedTileSourceKey = settings.MAP_TILE_SOURCES.get();

		int selectedItem = -1;
		if (!settings.MAP_ONLINE_DATA.get()) {
			selectedItem = 0;
		} else {

			Entry<String, String> selectedEntry = null;
			for (Entry<String, String> entry : entriesMap.entrySet()) {
				if (entry.getKey().equals(selectedTileSourceKey)) {
					selectedEntry = entry;
					break;
				}
			}
			if (selectedEntry != null) {
				selectedItem = 0;
				entriesMapList.remove(selectedEntry);
				entriesMapList.add(0, selectedEntry);
			}
		}

		final String[] items = new String[entriesMapList.size()];
		int i = 0;
		for (Entry<String, String> entry : entriesMapList) {
			items[i++] = entry.getValue();
		}

		builder.setSingleChoiceItems(items, selectedItem, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String layerKey = entriesMapList.get(which).getKey();
				switch (layerKey) {
					case layerOsmVector:
						settings.MAP_ONLINE_DATA.set(false);
						updateMapSource(mapView, null);
						break;
					case layerEditInstall:
						OsmandRasterMapsPlugin.defineNewEditLayer(activity, new ResultMatcher<TileSourceTemplate>() {

							@Override
							public boolean publish(TileSourceTemplate object) {
								settings.MAP_TILE_SOURCES.set(object.getName());
								settings.MAP_ONLINE_DATA.set(true);
								updateMapSource(mapView, settings.MAP_TILE_SOURCES);
								return true;
							}

							@Override
							public boolean isCancelled() {
								return false;
							}

						});
						break;
					case layerInstallMore:
						OsmandRasterMapsPlugin.installMapLayers(activity, new ResultMatcher<TileSourceTemplate>() {
							TileSourceTemplate template = null;
							int count = 0;

							@Override
							public boolean publish(TileSourceTemplate object) {
								if (object == null) {
									if (count == 1) {
										settings.MAP_TILE_SOURCES.set(template.getName());
										settings.MAP_ONLINE_DATA.set(true);
										updateMapSource(mapView, settings.MAP_TILE_SOURCES);
									} else {
										selectMapLayer(mapView);
									}
								} else {
									count++;
									template = object;
								}
								return false;
							}

							@Override
							public boolean isCancelled() {
								return false;
							}
						});
						break;
					default:
						settings.MAP_TILE_SOURCES.set(layerKey);
						settings.MAP_ONLINE_DATA.set(true);
						updateMapSource(mapView, settings.MAP_TILE_SOURCES);
						break;
				}

				dialog.dismiss();
			}

		});
		builder.show();
	}


	private String getString(int resId) {
		return activity.getString(resId);
	}

	public PointNavigationLayer getNavigationLayer() {
		return navigationLayer;
	}

	public ImpassableRoadsLayer getImpassableRoadsLayer() {
		return impassableRoadsLayer;
	}

	public GPXLayer getGpxLayer() {
		return gpxLayer;
	}

	public ContextMenuLayer getContextMenuLayer() {
		return contextMenuLayer;
	}

	public FavoritesLayer getFavoritesLayer() {
		return favoritesLayer;
	}

	public MapTextLayer getMapTextLayer() {
		return mapTextLayer;
	}

	public PointLocationLayer getLocationLayer() {
		return locationLayer;
	}

	public MapInfoLayer getMapInfoLayer() {
		return mapInfoLayer;
	}

	public MapControlsLayer getMapControlsLayer() {
		return mapControlsLayer;
	}

	public MapMarkersLayer getMapMarkersLayer() {
		return mapMarkersLayer;
	}

	public MapTileLayer getMapTileLayer() {
		return mapTileLayer;
	}

	public MapVectorLayer getMapVectorLayer() {
		return mapVectorLayer;
	}

	public POIMapLayer getPoiMapLayer() {
		return poiMapLayer;
	}

	public TransportInfoLayer getTransportInfoLayer() {
		return transportInfoLayer;
	}

	public DownloadedRegionsLayer getDownloadedRegionsLayer() {
		return downloadedRegionsLayer;
	}

	public interface DismissListener {
		void dismiss();
	}
}
