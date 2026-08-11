import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from './api.config';
import { Forecast } from './models/forecast';
import { Municipality } from './models/municipality';
import { WeatherApiService } from './weather-api.service';

describe('WeatherApiService', () => {
  let service: WeatherApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(WeatherApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('searchMunicipalities sends a GET to /municipalities with the name parameter', () => {
    const mockResponse: Municipality[] = [{ code: '28079', name: 'Madrid' }];
    let result: Municipality[] | undefined;

    service.searchMunicipalities('madrid').subscribe((response) => (result = response));

    const req = httpMock.expectOne(
      (request) => request.url === `${API_BASE_URL}/municipalities` && request.params.get('name') === 'madrid',
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);

    expect(result).toEqual(mockResponse);
  });

  it('builds the name parameter correctly for names with spaces and accents', () => {
    service.searchMunicipalities('San Sebastián').subscribe();

    const req = httpMock.expectOne(
      (request) =>
        request.url === `${API_BASE_URL}/municipalities` && request.params.get('name') === 'San Sebastián',
    );
    req.flush([]);
  });

  it('getForecast sends a GET to /forecast/{code} with temperatureUnit=G_CEL', () => {
    service.getForecast('44001', 'G_CEL').subscribe();

    const req = httpMock.expectOne(
      (request) =>
        request.url === `${API_BASE_URL}/forecast/44001` && request.params.get('temperatureUnit') === 'G_CEL',
    );
    expect(req.request.method).toBe('GET');
    req.flush({ mediaTemperatura: 25.5, unidadTemperatura: 'G_CEL', probPrecipitacion: [] });
  });

  it('getForecast sends a GET to /forecast/{code} with temperatureUnit=G_FAH', () => {
    service.getForecast('44001', 'G_FAH').subscribe();

    const req = httpMock.expectOne(
      (request) =>
        request.url === `${API_BASE_URL}/forecast/44001` && request.params.get('temperatureUnit') === 'G_FAH',
    );
    req.flush({ mediaTemperatura: 77.9, unidadTemperatura: 'G_FAH', probPrecipitacion: [] });
  });

  it('getForecast receives the Forecast response correctly typed', () => {
    const mockResponse: Forecast = {
      mediaTemperatura: 25.5,
      unidadTemperatura: 'G_CEL',
      probPrecipitacion: [{ probabilidad: 5, periodo: '00-06' }],
    };
    let result: Forecast | undefined;

    service.getForecast('44001', 'G_CEL').subscribe((response) => (result = response));

    const req = httpMock.expectOne((request) => request.url === `${API_BASE_URL}/forecast/44001`);
    req.flush(mockResponse);

    expect(result).toEqual(mockResponse);
  });
});
