package net.osmand.plus.mapcontextmenu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.sections.MenuController;
import net.osmand.plus.views.OsmandMapTileView;

import org.apache.commons.logging.Log;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static net.osmand.plus.mapcontextmenu.sections.MenuBuilder.SHADOW_HEIGHT_BOTTOM_DP;
import static net.osmand.plus.mapcontextmenu.sections.MenuBuilder.SHADOW_HEIGHT_TOP_DP;


public class MapContextMenuFragment extends Fragment {

	public static final String TAG = "MapContextMenuFragment";
	private static final Log LOG = PlatformUtil.getLog(MapContextMenuFragment.class);

	private View view;
	private View mainView;
	private View bottomView;
	private View shadowView;

	MenuController menuController;

	private int menuTopHeight;
	private int menuTopShadowHeight;
	private int menuTopShadowAllHeight;
	private int menuTitleHeight;
	private int menuButtonsHeight;
	private int menuBottomViewHeight;
	private int menuFullHeight;
	private int menuFullHeightMax;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		/*
		if(!portrait) {
			mapActivity.getMapView().setMapPositionX(1);
			mapActivity.getMapView().refreshMap();
		}

		if(!AndroidUiHelper.isXLargeDevice(mapActivity)) {
			AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_right_widgets_panel), false);
			AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_left_widgets_panel), false);
		}
		if(!portrait) {
			AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_route_land_left_margin), true);
		}
		*/
	}

	@Override
	public void onDetach() {
		super.onDetach();

		/*
		mapActivity.getMapView().setMapPositionX(0);
		mapActivity.getMapView().refreshMap();

		AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_route_land_left_margin), false);
		AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_right_widgets_panel), true);
		AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_left_widgets_panel), true);
		*/
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		getCtxMenu().saveMenuState(outState);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		if (savedInstanceState != null)
			getCtxMenu().restoreMenuState(savedInstanceState);

		view = inflater.inflate(R.layout.map_context_menu_fragment, container, false);

		ViewTreeObserver vto = view.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {

				menuTopHeight = view.findViewById(R.id.context_menu_top_view).getHeight();
				menuTopShadowHeight = view.findViewById(R.id.context_menu_top_shadow).getHeight();
				menuTopShadowAllHeight = view.findViewById(R.id.context_menu_top_shadow_all).getHeight();
				menuButtonsHeight = view.findViewById(R.id.context_menu_buttons).getHeight();
				menuFullHeight = view.findViewById(R.id.context_menu_main).getHeight();

				menuTitleHeight = menuTopShadowHeight + menuTopShadowAllHeight;
				menuFullHeightMax = menuTitleHeight + menuBottomViewHeight + dpToPx(2f);

				ViewTreeObserver obs = view.getViewTreeObserver();

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}

				doLayoutMenu();
			}

		});

		shadowView = view.findViewById(R.id.context_menu_shadow_view);
		shadowView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View view, MotionEvent event) {
				dismissMenu();
				return true;
			}
		});

		mainView = view.findViewById(R.id.context_menu_main);

		final View.OnTouchListener slideTouchListener = new View.OnTouchListener() {
			private float dy;
			private float dyMain;
			private VelocityTracker velocity;
			private boolean slidingUp;
			private boolean slidingDown;

			private float velocityX;
			private float velocityY;
			private float maxVelocityY;

			private float startX;
			private float startY;
			private long lastTouchDown;
			private final int CLICK_ACTION_THRESHHOLD = 200;

			private boolean isClick(float endX, float endY) {
				float differenceX = Math.abs(startX - endX);
				float differenceY = Math.abs(startY - endY);
				if (differenceX > 1 ||
						differenceY > 1 ||
						Math.abs(velocityX) > 10 ||
						Math.abs(velocityY) > 10 ||
						System.currentTimeMillis() - lastTouchDown > CLICK_ACTION_THRESHHOLD) {
					return false;
				}
				return true;
			}

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						startX = event.getX();
						startY = event.getY();
						lastTouchDown = System.currentTimeMillis();

						dy = event.getY();
						dyMain = mainView.getY();
						velocity = VelocityTracker.obtain();
						velocityX = 0;
						velocityY = 0;
						maxVelocityY = 0;
						velocity.addMovement(event);
						break;

					case MotionEvent.ACTION_MOVE:
						float y = event.getY();
						float newY = mainView.getY() + (y - dy);
						mainView.setY((int)newY);

						menuFullHeight = view.getHeight() - (int) newY + 10;
						ViewGroup.LayoutParams lp = mainView.getLayoutParams();
						lp.height = Math.max(menuFullHeight, menuTitleHeight);
						mainView.setLayoutParams(lp);
						mainView.requestLayout();

						velocity.addMovement(event);
						velocity.computeCurrentVelocity(1000);
						velocityX = Math.abs(velocity.getXVelocity());
						velocityY = Math.abs(velocity.getYVelocity());
						if (velocityY > maxVelocityY)
							maxVelocityY = velocityY;

						break;

					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						float endX = event.getX();
						float endY = event.getY();

						slidingUp = Math.abs(maxVelocityY) > 500 && (mainView.getY() - dyMain) < -50;
						slidingDown = Math.abs(maxVelocityY) > 500 && (mainView.getY() - dyMain) > 50;

						velocity.recycle();

						if (menuController != null) {
							if (slidingUp) {
								menuController.slideUp();
							} else if (slidingDown) {
								menuController.slideDown();
							}
						}

						final int posY = getPosY();

						if (mainView.getY() != posY) {

							if (posY < mainView.getY()) {
								updateMainViewLayout(posY);
							}

							mainView.animate().y(posY)
									.setDuration(200)
									.setInterpolator(new DecelerateInterpolator())
									.setListener(new AnimatorListenerAdapter() {
										@Override
										public void onAnimationCancel(Animator animation) {
											updateMainViewLayout(posY);
										}

										@Override
										public void onAnimationEnd(Animator animation) {
											updateMainViewLayout(posY);
										}
									})
									.start();
						}

						// OnClick event
						if (isClick(endX, endY)) {
							OsmandMapTileView mapView = getMapActivity().getMapView();
							mapView.getAnimatedDraggingThread().startMoving(getCtxMenu().getPointDescription().getLat(), getCtxMenu().getPointDescription().getLon(),
									mapView.getZoom(), true);
						}

						break;

				}
				return true;
			}
		};

		View topView = view.findViewById(R.id.context_menu_top_view);
		topView.setOnTouchListener(slideTouchListener);
		View topShadowView = view.findViewById(R.id.context_menu_top_shadow);
		topShadowView.setOnTouchListener(slideTouchListener);
		View topShadowAllView = view.findViewById(R.id.context_menu_top_shadow_all);
		topShadowAllView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getY() <= dpToPx(SHADOW_HEIGHT_TOP_DP) || event.getAction() != MotionEvent.ACTION_DOWN)
					return slideTouchListener.onTouch(v, event);
				else
					return false;
			}
		});

		// Left icon
		IconsCache iconsCache = getMyApplication().getIconsCache();
		boolean light = getMyApplication().getSettings().isLightContent();

		int iconId = getCtxMenu().getLeftIconId();

		final View iconLayout = view.findViewById(R.id.context_menu_icon_layout);
		final ImageView iconView = (ImageView)view.findViewById(R.id.context_menu_icon_view);
		if (iconId == 0) {
			iconLayout.setVisibility(View.GONE);
		} else {
			iconView.setImageDrawable(iconsCache.getIcon(iconId,
					light ? R.color.osmand_orange : R.color.osmand_orange_dark));
		}

		// Text line 1
		TextView line1 = (TextView) view.findViewById(R.id.context_menu_line1);
		line1.setText(getCtxMenu().getAddressStr());

		// Text line 2
		TextView line2 = (TextView) view.findViewById(R.id.context_menu_line2);
		line2.setText(getCtxMenu().getLocationStr(getMapActivity()));

		// Close button
		final ImageView closeButtonView = (ImageView)view.findViewById(R.id.context_menu_close_btn_view);
		closeButtonView.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_remove_dark,
				light ? R.color.icon_color_light : R.color.dash_search_icon_dark));
		closeButtonView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((MapActivity) getActivity()).getMapLayers().getContextMenuLayer().hideMapContextMenuMarker();
				dismissMenu();
			}
		});

		// Action buttons
		final ImageButton buttonNavigate = (ImageButton) view.findViewById(R.id.context_menu_route_button);
		buttonNavigate.setImageDrawable(iconsCache.getIcon(R.drawable.map_directions,
				light ? R.color.icon_color : R.color.dashboard_subheader_text_dark));
		buttonNavigate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getCtxMenu().buttonNavigatePressed(getMapActivity());
			}
		});

		final ImageButton buttonFavorite = (ImageButton) view.findViewById(R.id.context_menu_fav_button);
		buttonFavorite.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_fav_dark,
				light ? R.color.icon_color : R.color.dashboard_subheader_text_dark));
		buttonFavorite.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getCtxMenu().buttonFavoritePressed(getMapActivity());
			}
		});

		final ImageButton buttonShare = (ImageButton) view.findViewById(R.id.context_menu_share_button);
		buttonShare.setImageDrawable(iconsCache.getIcon(R.drawable.abc_ic_menu_share_mtrl_alpha,
				light ? R.color.icon_color : R.color.dashboard_subheader_text_dark));
		buttonShare.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getCtxMenu().buttonSharePressed(getMapActivity());
			}
		});

		final ImageButton buttonMore = (ImageButton) view.findViewById(R.id.context_menu_more_button);
		buttonMore.setImageDrawable(iconsCache.getIcon(R.drawable.ic_overflow_menu_white,
				light ? R.color.icon_color : R.color.dashboard_subheader_text_dark));
		buttonMore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getCtxMenu().buttonMorePressed(getMapActivity());
			}
		});

		// Menu controller
		menuController = getCtxMenu().getMenuController();
		bottomView = view.findViewById(R.id.context_menu_bottom_view);
		if (menuController != null) {
			bottomView.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return true;
				}
			});
			menuController.build(bottomView);
		}

		bottomView.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		menuBottomViewHeight = bottomView.getMeasuredHeight();

		return view;
	}

	private int getPosY() {
		int destinationState;
		int minHalfY;
		if (menuController != null) {
			destinationState = menuController.getCurrentMenuState();
			minHalfY = view.getHeight() - (int)(view.getHeight() * menuController.getHalfScreenMaxHeightKoef());
		} else {
			destinationState = MenuController.MenuState.HEADER_ONLY;
			minHalfY = view.getHeight();
		}

		int posY = 0;
		switch (destinationState) {
			case MenuController.MenuState.HEADER_ONLY:
				posY = view.getHeight() - (menuTitleHeight - dpToPx(SHADOW_HEIGHT_BOTTOM_DP));
				break;
			case MenuController.MenuState.HALF_SCREEN:
				posY = view.getHeight() - menuFullHeightMax;
				posY = Math.max(posY, minHalfY);
				break;
			case MenuController.MenuState.FULL_SCREEN:
				posY = -menuTopShadowHeight - dpToPx(SHADOW_HEIGHT_TOP_DP);
				break;
			default:
				break;
		}
		return posY;
	}

	private void updateMainViewLayout(int posY) {
		ViewGroup.LayoutParams lp;
		menuFullHeight = view.getHeight() - posY;
		lp = mainView.getLayoutParams();
		lp.height = Math.max(menuFullHeight, menuTitleHeight);
		mainView.setLayoutParams(lp);
		mainView.requestLayout();
	}

	private void doLayoutMenu() {
		final int posY = getPosY();
		mainView.setY(posY);
		updateMainViewLayout(posY);
	}

	public void dismissMenu() {
		getActivity().getSupportFragmentManager().popBackStack();
	}

	public OsmandApplication getMyApplication() {
		if (getActivity() == null) {
			return null;
		}
		return (OsmandApplication) getActivity().getApplication();
	}

	public static void showInstance(final MapActivity mapActivity) {
		MapContextMenuFragment fragment = new MapContextMenuFragment();
		mapActivity.getSupportFragmentManager().beginTransaction()
				.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom)
				.add(R.id.fragmentContainer, fragment, "MapContextMenuFragment")
				.addToBackStack(null).commit();
	}

	private MapContextMenu getCtxMenu() {
		return getMapActivity().getContextMenu();
	}

	private MapActivity getMapActivity() {
		return (MapActivity)getActivity();
	}

	// Utils
	private int getScreenHeight() {
		DisplayMetrics dm = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}

	private int dpToPx(float dp) {
		Resources r = getActivity().getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}
}

