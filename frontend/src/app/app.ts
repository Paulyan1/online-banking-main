import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {
  message = 'loading...';

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    console.log('ngOnInit called');

    this.http.get('http://localhost:8080/api/hello', { responseType: 'text' })
      .subscribe({
        next: data => {
          console.log(data);
          this.message = data;
          this.cdr.detectChanges();
        },
        error: err => {
          console.error(err);
          this.message = 'Failed to call backend';
          this.cdr.detectChanges();
        }
      });
  }
}