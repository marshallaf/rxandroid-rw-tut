/*
 * Copyright (c) 2016 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.cheesefinder;

import android.view.View;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;

public class CheeseActivity extends BaseSearchActivity {

    // returns an observable that emits strings
    private Observable<String> createButtonClickObservable() {
        // create and return a new Observable
        return Observable.create(new ObservableOnSubscribe<String>() {
            // this defines what happens when it is subscribed to
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                // when the click event happens, call onNext on the emitter
                mSearchButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        emitter.onNext(mQueryEditText.getText().toString());
                    }
                });

                // this prevents memory leak by removing the reference to the listener
                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        mSearchButton.setOnClickListener(null);
                    }
                });
            }
        });
    }

    protected void onStart() {
        super.onStart();

        // creates the observable
        Observable<String> searchTextObservable = createButtonClickObservable();

        // subscribe to the observable with a simple consumer
        searchTextObservable.subscribe(new Consumer<String>() {
            // accept() will be called when the observable emits an item
            @Override
            public void accept(String query) throws Exception {
                // perform search and show the results
                showResult(mCheeseSearchEngine.search(query));
            }
        });
    }
}
