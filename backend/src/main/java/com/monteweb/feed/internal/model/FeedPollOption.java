package com.monteweb.feed.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "feed_poll_options")
@Getter
@Setter
@NoArgsConstructor
public class FeedPollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private FeedPoll poll;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(nullable = false)
    private int position = 0;
}
