package org.greencheek.caching.herdcache;

import org.greencheek.caching.herdcache.domain.CacheItem;
import rx.Observable;
import rx.Scheduler;
import rx.Single;
import rx.SingleSubscriber;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

import java.util.Optional;
import java.util.UUID;

/**
 * Created by dominictootell on 20/09/2016.
 */
public class RxHacking {


    public static Single<String> set(final String value) {

        Single<String> single =  Single.create(s -> {
            System.out.println("----- Sleeping set value (" + Thread.currentThread().getName() + ") -----");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();

            }
            System.out.println("----- Sleeping Set Value (" + value + ") on (" + Thread.currentThread().getName() + ") -----");
            System.out.println("----- Finished Sleeping set value(" + Thread.currentThread().getName() + ") -----");
            s.onSuccess(value);
        });

        return single;
    }

    public static void main(String[] args) {
////        final CountDownLatch latch = new CountDownLatch(1);
////        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
////        final GuavaSettableFuture<String> f = new GuavaSettableFuture<>();
////
////        executorService.submit(() -> {
////            System.out.println("Things to do");
////            try {
////                Thread.sleep(10000);
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
////            f.set("yo");
////            latch.countDown();
////
////        });
//////
//////        f.addListener(() -> {
//////            try {
//////                System.out.println("------\nfired\n"+f.get()+"\n-----");
//////            } catch (InterruptedException e) {
//////                e.printStackTrace();
//////            } catch (ExecutionException e) {
//////                e.printStackTrace();
//////            }
//////        },executorService);
//////
////
//////        f.addListener(() -> {
//////            try {
//////                System.out.println("------\nfired 2\n"+f.get()+"\n-----");
//////            } catch (InterruptedException e) {
//////                e.printStackTrace();
//////            } catch (ExecutionException e) {
//////                e.printStackTrace();
//////            }
//////        },executorService);
////
////
////        Scheduler sc = Schedulers.from(executorService);
////
////        Observable<String> o = Observable.from(f, sc);
////        o.single().subscribe(s -> System.out.println("value: "+s));
////
////        try {
////            latch.await();
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
////
////
////
////
////        Observable<String> o2 = Observable.from(f, sc);
////        o2.single().subscribe(s -> System.out.println("value2: "+s));
////
////        //===
////
////        CountDownLatch latch2 = new CountDownLatch(1);
////        AsyncSubject<String> subject = AsyncSubject.create();
//////        Observable<String> subject = sub.observeOn(sc);
////
//////        Observable.create(new Observable.OnSubscribe<String>() {
//////            @Override
//////            public void call(Subscriber<? super String> subscriber) {
//////                try {
//////                    subscriber.onNext("jjj");    // Pass on the data to subscriber
//////                    subscriber.onCompleted();     // Signal about the completion subscriber
//////                } catch (Exception e) {
//////                    subscriber.onError(e);        // Signal about the error to subscriber
//////                }
//////            }
//////        });
////////
////        executorService.submit(() -> {
////            System.out.println("full rx");
////
////            try {
////                Thread.sleep(10000);
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
////
////            latch2.countDown();
////            subject.onNext("value");
////            subject.onCompleted();
////        });
////
////        System.out.println("rx1");
////        subject.single().subscribe(
////                new Action1<String>() {
////                    @Override
////                    public void call(String s) {
////                        System.out.println("rx value 1: " + s + " " + Thread.currentThread().getName());
////                    }
////                });
////
////
////        System.out.println("rx2");
////        subject.single().subscribe(s -> System.out.println("rx value 2: "+s + " " + Thread.currentThread().getName()));
////
////        System.out.println("======");
////        try {
////            latch2.await();
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
////        System.out.println("latch done ");
////        System.out.println("======");
////
////        System.out.println("rx3");
////        subject.single().subscribe(s -> System.out.println("rx value 3: "+s + " " + Thread.currentThread().getName()));
////        System.out.println("rx4");
////        subject.single().subscribe(s -> System.out.println("rx value 4: "+s + " " + Thread.currentThread().getName()));
////        System.out.println("rx5");
////        subject.single().subscribe(s -> System.out.println("rx value 5: "+s + " " + Thread.currentThread().getName()));
////        System.out.println("rx6");
////
////        try {
////            Thread.sleep(10000);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
////        subject.single().subscribe(s -> System.out.println("rx value 6: "+s + " " + Thread.currentThread().getName()));
////        subject.single().subscribe(s -> System.out.println("rx value 7: "+s + " " + Thread.currentThread().getName()));
////        subject.single().subscribe(s -> System.out.println("rx value 8: "+s + " " + Thread.currentThread().getName()));
////        subject.single().subscribe(s -> System.out.println("rx value 9: "+s + " " + Thread.currentThread().getName()));
////
//
////        Observable<String> ob = Observable.just("String value").single();
////
////        ob.subscribe(System.out::println);
////        ob.subscribe(System.out::println);
////        ob.subscribe(System.out::println);
////        ob.subscribe(System.out::println);
////        ob.subscribe(System.out::println);
////
////        try {
////            Thread.sleep(10000);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
////        ob.subscribe(System.out::println);
//
////        executorService.shutdownNow();
//
//        // Single 199
//        // 54
//        //150
        Single<String> obser = Single.create(new Single.OnSubscribe<String>() {
                                                   @Override
                                                   public void call(SingleSubscriber<? super String> singleSubscriber) {
                                                       System.out.println("starting:" + Thread.currentThread().getName());
                                                       singleSubscriber.onSuccess("ff:" + Thread.currentThread().getName() + UUID.randomUUID().toString());
                                                   }
                                               }
        ).toObservable().cache().subscribeOn(Schedulers.io()).toSingle();

//        Observable<String> obs2 = Single.create(new Single.OnSubscribe<String>() {
//                                                    @Override
//                                                    public void call(SingleSubscriber<? super String> singleSubscriber) {
//                                                        System.out.println("starting 2:"  + Thread.currentThread().getName());
//                                                        singleSubscriber.onSuccess("ff 2:" + Thread.currentThread().getName());
//                                                    }
//                                                }
//        ).toObservable().cacheWithInitialCapacity(1).subscribeOn(Schedulers.io());
//
//        Observable<String> obs3 = Single.create(new Single.OnSubscribe<String>() {
//                                                    @Override
//                                                    public void call(SingleSubscriber<? super String> singleSubscriber) {
//                                                        System.out.println("starting 2:"  + Thread.currentThread().getName());
//                                                        singleSubscriber.onSuccess("ff 2:" + Thread.currentThread().getName());
//                                                        singleSubscriber.add(Subscriptions.create(() -> {
//                                                            System.out.println("closed");
//                                                        }));
//                                                    }
//                                                }
//        ).toObservable().cacheWithInitialCapacity(1).subscribeOn(Schedulers.immediate());


////        obs.subscribe();
        obser.subscribe(System.out::println);
        obser.subscribe(System.out::println);
//
////        obs.connect();
//        obs2.subscribe(System.out::println);
//
//
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        obs3.subscribe(System.out::println);


//        Single<CacheItem<String>> s = Single.create(sub -> {
//            System.out.println("started");
//            sub.onSuccess(new CacheItem<String>("success",true));
//        });
//
//        Observable<CacheItem<String>> obs9 = s.toObservable().cacheWithInitialCapacity(1);
//        s = obs9.toSingle();
//
//        s.subscribe(cachedItem -> {
//            if(cachedItem.isFromCache()) {
//                String j = cachedItem.getValue().get();
//                if(j.equals("success")) {
//                    System.out.println("resetting");
//                }
//            }
//        });
//
//        s.observeOn(Schedulers.io())
//        .subscribe(cachedItem -> {
//            if(cachedItem.isFromCache()) {
//                String j = cachedItem.getValue().get();
//                if(j.equals("success")) {
//                    System.out.println("resetting:" + Thread.currentThread().getName());
//                }
//            }
//        });
//        s.observeOn(Schedulers.immediate())
//        .subscribe(cachedItem -> {
//            Optional<String> item = cachedItem.getValue();
//            if(item.isPresent()) {
//                System.out.println("using value:" + Thread.currentThread().getName());
//            }
//        });
//


        final boolean wait = false;
        Single<String> s1 = Single.create(sub -> {
            System.out.println("----- Getting Value ("+ Thread.currentThread().getName() + ") -----");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();

            }
            System.out.println("----- Got Value  ("+ Thread.currentThread().getName() + ") -----");
            String uuid = UUID.randomUUID().toString();
            sub.onSuccess(uuid);


            if(!wait) {
                set(uuid).subscribeOn(Schedulers.newThread()).subscribe();
            } else {
                set(uuid).subscribeOn(Schedulers.immediate()).subscribe();
            }

        });

        Single<String> s2 = s1.toObservable().cacheWithInitialCapacity(1).toSingle();
//        s2 = s2.subscribeOn(Schedulers.computation());





//        Observable<String> obs = s2;
//        if(!wait) {
////            Observable<String> writeValue = s2.toObservable();
////            Observable<String> withValue = writeValue.filter(value -> value != null);
//            obs = s2.doOnNext(value -> set(value).subscribeOn(Schedulers.newThread()).subscribe());
//                    System.out.println("ldkfjasdlkfjsdalkfj");
////            obs = s2.flatMap(v -> set(v));
//
////            writeValue.filter()
////            withValue.subscribe(value -> set(value).subscribeOn(Schedulers.io()).subscribe());
//        }
//        else {
////            Observable<String> writeValue = s2.toObservable();
//            Observable<String> withValue = s2.filter(value -> value != null);
//            obs = withValue.doOnNext(value -> set(value).subscribeOn(Schedulers.immediate()).subscribe());
//
////            obs = withValue.flatMap(value -> set(value));
////            obs = withValue.observeOn(Schedulers.io());
//        }
//
//
////            setValueInCache = setValueInCache.subscribeOn(Schedulers.immediate());
//        } else {
//
//            Observable<String> writeValue2 = withValue.concatMap(value -> set(value));
//            writeValue2.subscribe();
////            setValueInCache = setValueInCache.subscribeOn(Schedulers.io());
////            System.out.println("lkjlkjlkj");
//        }



        System.out.println("Kicking of sub");
        s2 = s2.subscribeOn(Schedulers.io());
        s2 = s2.observeOn(Schedulers.computation());
//        obs.observeOn(Schedulers.io());
        s2.subscribe(val -> System.out.println("----- Got Value to use (" + val + ")(" + Thread.currentThread().getName() + ") -----"));

//        setValueInCache = setValueInCache.observeOn(Schedulers.io());

//        setValueInCache.subscribe();



//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        s2.subscribe(val -> System.out.println("----- Got Value2 to use (" + val + ")(" + Thread.currentThread().getName() + ") -----"));

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}