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

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

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
        Observable<String> searchTextObservable = createTextChangeObservable();

        // subscribe to the observable with a simple consumer
        searchTextObservable
                // start on UI thread for progress bar
                .observeOn(AndroidSchedulers.mainThread())
                // doOnNext() is called every time an item is emitted by an observable
                .doOnNext(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        showProgressBar();
                    }
                })
                // switch to IO thread
                .observeOn(Schedulers.io())
                // for each query, return a list of results
                .map(new Function<String, List<String>>() {
                    @Override
                    public List<String> apply(String query) throws Exception {
                        return mCheeseSearchEngine.search(query);
                    }
                })
                // anything after this will be called on the UI thread
                .observeOn(AndroidSchedulers.mainThread())
                // since our showResult() call there updates the UI, it has to be on the UI thread
                .subscribe(new Consumer<List<String>>() {
                    // accept() will be called when the observable emits an item
                    @Override
                    public void accept(List<String> result) throws Exception {
                        // perform search and show the results
                        hideProgressBar();
                        showResult(result);
                    }
                });
    }

    private Observable<String> createTextChangeObservable() {
        // create an observable
        Observable<String> textChangeObservable = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                // when an observer subscribes, make a TextWatcher
                final TextWatcher watcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    // when the text changes, emit the new sequence
                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        emitter.onNext(charSequence.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                };

                // add the watcher to the query text view
                mQueryEditText.addTextChangedListener(watcher);

                // make sure we don't keep any references
                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        mQueryEditText.removeTextChangedListener(watcher);
                    }
                });
            }
        });
        // return the observable, with a filter such that queries of size == 1 don't get propagated further
        return textChangeObservable.filter(new Predicate<String>() {
            @Override
            public boolean test(String s) throws Exception {
                return s.length() >= 2;
            }
        }).debounce(500, TimeUnit.MILLISECONDS);
    }
}
