package com.material.management;

import android.os.SystemClock;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.material.management.data.StreamItem;

import org.hamcrest.Description;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MaterialManagerExpressoTest {
    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    @Test
    public void triggleTest1SlideMenu() {
        for (int i = 0; i < 4; i++) {
            onView(withClassName(endsWith("ImageButton"))).perform(click()).check(matches(isDisplayed()));
            SystemClock.sleep(1000);
        }
    }

    @Test
    public void triggleTest2MaterialManagement() {
        /* open the MaterialManagerFragment*/
        onView(withClassName(endsWith("ImageButton"))).perform(click()).check(matches(isDisplayed()));
        SystemClock.sleep(1000);
        onView(withText(R.string.slidemenu_material_view_title)).perform(click());
        onView(withId(R.id.gv_material_grid)).check(matches(isDisplayed()));
        SystemClock.sleep(1000);

        /* Scroll test */
        onData(is(new BoundedMatcher<Object, StreamItem>(StreamItem.class) {
            @Override
            protected boolean matchesSafely(StreamItem item) {
                return item.getMaterialType().equals("test");
            }

            @Override
            public void describeTo(Description description) {}


        })).inAdapterView(withId(R.id.gv_material_grid)).atPosition(0).check(matches(isDisplayed()));
        SystemClock.sleep(2000);
    }
}
