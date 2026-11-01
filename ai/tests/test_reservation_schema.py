from schemas.reservation import ReservationExtraction


def test_reservation_extraction_accepts_null_device_names() -> None:
    extraction = ReservationExtraction.model_validate(
        {
            "start_time": None,
            "end_time": None,
            "participant_count": 2,
            "experiment_name": None,
            "room_type": "化学实验室",
            "room_name": None,
            "device_names": None,
        }
    )

    assert extraction.device_names == []
