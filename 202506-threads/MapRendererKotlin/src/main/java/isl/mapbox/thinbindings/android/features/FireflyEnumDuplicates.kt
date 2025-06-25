package isl.mapbox.thinbindings.android.features

// These enums are duplicates of those in ISL.Firefly.Datatypes

//
// Summary:
//     Travel with regards to the topological direction of the line.
public enum class DirectionOfTravel
{
    //
    // Summary:
    //     With the toplogical direction of the line
    With,
    //
    // Summary:
    //     Against the topological direction of the line
    Against
}

//
public enum class DirectionsOfService
{
    // Summary:
    //     Used where a segment has no service related to it. Such as non-productive travel.
    None, // = 0x0,
    //
    // Summary:
    //     Can only be serviced when travelling with the toplogical direction of the line
    With, // = 0x1,
    //
    // Summary:
    //     Can only be serviced when travelling against the topological direction of the
    //     line
    Against, // = 0x2,
    //
    // Summary:
    //     Can be serviced when travelling in either direction along the line
    //Either = 0x3,
    Both, // = 0x3
}

//
// Summary:
//     The statues that a JobInstance can be in at any given time.
public enum class JobStatus
{
    //
    // Summary:
    //     Unknown
    Unknown, // = 0,
    //
    // Summary:
    //     Awaiting
    Pending, // = 2,
    //
    // Summary:
    //     Once a job is actived by an operator, so that they confirm they are going to
    //     work the job
    Enroute, // = 4,
    //
    // Summary:
    //     Whilst carrying out the job
    InProgress, // = 8,
    //
    // Summary:
    //     Once the job work has been done
    Finished, // = 0x10
}

public enum class ManoeuvreType(val value: Int)
{
    /// <summary>
    /// No action
    /// </summary>
    None(0),

    /// <summary>
    /// Continuing in same direction
    /// </summary>
    Straight(0x01),

    /// <summary>
    /// Turn in the road
    /// </summary>
    UTurn(0x02),

    /// <summary>
    /// Small turn, bear left
    /// </summary>
    SlightLeft(0x11),

    /// <summary>
    /// Anti-Clockwise Turn
    /// </summary>
    TurnLeft(0x12),

    /// <summary>
    /// Tight left turn
    /// </summary>
    SharpLeft(0x13),

    /// <summary>
    /// Slight right turn
    /// </summary>
    SlightRight(0x21),

    /// <summary>
    /// Clockwise turn
    /// </summary>
    TurnRight(0x22),

    /// <summary>
    /// Tight right turn
    /// </summary>
    SharpRight(0x23),

    /// <summary>
    /// Exiting from road onto slip road (Ramp)
    /// </summary>
    SlipRoadEnter(0x30),

    /// <summary>
    /// Joining road from a slip road (Ramp)
    /// </summary>
    SlipRoadExit(0x31),

    /// <summary>
    /// Merge with another traffic lane
    /// </summary>
    Merge(0x32),

    /// <summary>
    /// Fork to one side or other, direction unknown
    /// </summary>
    Fork(0x33),

    /// <summary>
    /// Fork to the left side
    /// </summary>
    ForkLeft(0x34),

    /// <summary>
    /// Fork to the right side
    /// </summary>
    ForkRight(0x35),

    /// <summary>
    /// At fork, take the middle road
    /// </summary>
    ForkMiddle(0x36),

    /// <summary>
    /// Joining a roundabout
    /// </summary>
    RoundaboutEnter(0x40),

    /// <summary>
    /// Leaving a roundabout
    /// </summary>
    RoundaboutExit(0x41),

    /// <summary>
    /// Occurs when arriving at a facility during a route
    /// </summary>
    ArriveAtFacility(0xF0),

    /// <summary>
    /// Arrive at the end of the planned route.  There should be no following route segments.
    /// </summary>
    ArriveAtEnd(0xFF);
    companion object {
        fun getByValue(value: Int) = values().firstOrNull { it.value == value }
    }
}
